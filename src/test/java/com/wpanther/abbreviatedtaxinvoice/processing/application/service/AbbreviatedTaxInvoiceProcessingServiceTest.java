package com.wpanther.abbreviatedtaxinvoice.processing.application.service;

import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.CompensateAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.ProcessAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.out.AbbreviatedTaxInvoiceEventPublishingPort;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.out.SagaReplyPort;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceProcessedDomainEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out.AbbreviatedTaxInvoiceParserPort;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out.ProcessedAbbreviatedTaxInvoiceRepository;
import com.wpanther.saga.domain.enums.SagaStep;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbbreviatedTaxInvoiceProcessingServiceTest {

    @Mock private ProcessedAbbreviatedTaxInvoiceRepository invoiceRepository;
    @Mock private AbbreviatedTaxInvoiceParserPort parserPort;
    @Mock private AbbreviatedTaxInvoiceEventPublishingPort eventPublisher;
    @Mock private SagaReplyPort sagaReplyPort;
    @Mock private PlatformTransactionManager transactionManager;

    private AbbreviatedTaxInvoiceProcessingService service;
    private ProcessedAbbreviatedTaxInvoice validInvoice;

    @BeforeEach
    void setUp() {
        service = new AbbreviatedTaxInvoiceProcessingService(
            invoiceRepository, parserPort, eventPublisher, sagaReplyPort,
            new SimpleMeterRegistry(), transactionManager);

        Party seller = Party.of("Seller Company",
            TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 Street", "Bangkok", "10110", "TH"));
        LineItem item = new LineItem("Service 1", 10,
            Money.of(new BigDecimal("1000.00"), "THB"), new BigDecimal("7.00"));
        validInvoice = ProcessedAbbreviatedTaxInvoice.builder()
            .id(AbbreviatedTaxInvoiceId.generate())
            .sourceInvoiceId("intake-123")
            .invoiceNumber("ABR-001")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller).addItem(item)
            .currency("THB").originalXml("<xml>test</xml>")
            .build();
    }

    @Test
    void process_success_savesCompletesPublishesReplies() throws Exception {
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserPort.parse(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any())).thenReturn(validInvoice);

        service.process("intake-123", "<xml/>", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");

        verify(parserPort).parse("<xml/>", "intake-123");
        verify(invoiceRepository, times(2)).save(any());
        verify(eventPublisher).publish(any(AbbreviatedTaxInvoiceProcessedDomainEvent.class));
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
    }

    @Test
    void process_alreadyCompleted_idempotentSuccess() throws Exception {
        ProcessedAbbreviatedTaxInvoice completed = buildWithStatus(ProcessingStatus.COMPLETED);
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.of(completed));

        service.process("intake-123", "<xml/>", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");

        verify(parserPort, never()).parse(anyString(), anyString());
        verify(invoiceRepository, never()).save(any());
        verify(eventPublisher, never()).publish(any());
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
    }

    @Test
    void process_foundInProcessingState_resumesCompletion() throws Exception {
        ProcessedAbbreviatedTaxInvoice processing = buildWithStatus(ProcessingStatus.PROCESSING);
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.of(processing));

        service.process("intake-123", "<xml/>", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");

        verify(parserPort, never()).parse(anyString(), anyString());
        verify(invoiceRepository, times(1)).save(any());
        verify(eventPublisher).publish(any(AbbreviatedTaxInvoiceProcessedDomainEvent.class));
        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
        assertEquals(ProcessingStatus.COMPLETED, processing.getStatus());
    }

    @Test
    void process_parseError_publishesFailureAndThrows() throws Exception {
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserPort.parse(anyString(), anyString()))
            .thenThrow(AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.forEmpty());

        assertThrows(ProcessAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceProcessingException.class,
            () -> service.process("intake-123", "", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"));

        verify(sagaReplyPort).publishFailure(eq("saga-1"), eq(SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE),
            eq("corr-1"), contains("Parse error"));
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void process_raceConditionResolved_publishesSuccess() throws Exception {
        SQLException sqlCause = new SQLException(
            "ERROR: duplicate key value violates unique constraint"
            + " \"uq_processed_abbreviated_tax_invoices_source_invoice_id\"", "23505");
        when(transactionManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));
        when(invoiceRepository.findBySourceInvoiceId(anyString()))
            .thenReturn(Optional.empty())
            .thenReturn(Optional.of(validInvoice));
        when(parserPort.parse(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any())).thenThrow(new DuplicateKeyException("dup key", sqlCause));

        assertThrows(ProcessAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceProcessingException.class,
            () -> service.process("intake-123", "<xml/>", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"));

        verify(sagaReplyPort).publishSuccess("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
        verify(sagaReplyPort, never()).publishFailure(any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any());
    }

    @Test
    void process_duplicateKeyOnOtherConstraint_publishesFailureNoRecheck() throws Exception {
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserPort.parse(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any()))
            .thenThrow(new DuplicateKeyException("duplicate key value violates unique constraint \"idx_abbr_invoice_number\""));

        assertThrows(ProcessAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceProcessingException.class,
            () -> service.process("intake-123", "<xml/>", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"));

        verify(sagaReplyPort).publishFailure(eq("saga-1"), any(), any(), contains("Constraint violation"));
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    void process_dataIntegrityViolation_publishesFailureImmediately() throws Exception {
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserPort.parse(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any()))
            .thenThrow(new DataIntegrityViolationException("value too long"));

        assertThrows(ProcessAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceProcessingException.class,
            () -> service.process("intake-123", "<xml/>", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"));

        verify(sagaReplyPort).publishFailure(eq("saga-1"), any(), any(), contains("Constraint violation"));
        verify(transactionManager, never()).getTransaction(any());
    }

    @Test
    void compensate_invoiceFound_deletesAndPublishesCompensated() {
        when(invoiceRepository.findBySourceInvoiceId("intake-123")).thenReturn(Optional.of(validInvoice));

        service.compensate("intake-123", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");

        verify(invoiceRepository).deleteById(validInvoice.getId());
        verify(sagaReplyPort).publishCompensated("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
    }

    @Test
    void compensate_invoiceNotFound_idempotentPublishesCompensated() {
        when(invoiceRepository.findBySourceInvoiceId("intake-notfound")).thenReturn(Optional.empty());

        service.compensate("intake-notfound", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");

        verify(invoiceRepository, never()).deleteById(any());
        verify(sagaReplyPort).publishCompensated("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
    }

    @Test
    void compensate_deleteThrows_publishesFailureAndRethrows() {
        when(invoiceRepository.findBySourceInvoiceId("intake-123")).thenReturn(Optional.of(validInvoice));
        doThrow(new RuntimeException("DB down")).when(invoiceRepository).deleteById(any());

        assertThrows(CompensateAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceCompensationException.class,
            () -> service.compensate("intake-123", "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"));

        verify(sagaReplyPort).publishFailure(eq("saga-1"), any(), any(), contains("Compensation failed"));
    }

    // --- helper ---
    private ProcessedAbbreviatedTaxInvoice buildWithStatus(ProcessingStatus status) {
        Party seller = Party.of("Seller", TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 St", "Bangkok", "10110", "TH"));
        LineItem item = new LineItem("Service", 10,
            Money.of(new BigDecimal("1000.00"), "THB"), new BigDecimal("7.00"));
        return ProcessedAbbreviatedTaxInvoice.builder()
            .id(AbbreviatedTaxInvoiceId.generate())
            .sourceInvoiceId("intake-123")
            .invoiceNumber("ABR-001")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller).addItem(item)
            .currency("THB").originalXml("<xml>test</xml>")
            .status(status)
            .build();
    }
}
