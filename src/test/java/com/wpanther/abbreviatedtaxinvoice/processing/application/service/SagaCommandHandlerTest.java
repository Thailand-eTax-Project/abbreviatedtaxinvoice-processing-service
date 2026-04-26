package com.wpanther.abbreviatedtaxinvoice.processing.application.service;

import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.CompensateAbbreviatedTaxInvoiceCommand;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.ProcessAbbreviatedTaxInvoiceCommand;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.repository.ProcessedAbbreviatedTaxInvoiceRepository;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.service.AbbreviatedTaxInvoiceParserService;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging.SagaReplyPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaCommandHandlerTest {

    @Mock
    private AbbreviatedTaxInvoiceProcessingService processingService;

    @Mock
    private SagaReplyPublisher sagaReplyPublisher;

    @Mock
    private ProcessedAbbreviatedTaxInvoiceRepository invoiceRepository;

    @InjectMocks
    private SagaCommandHandler handler;

    private ProcessAbbreviatedTaxInvoiceCommand validCommand;
    private CompensateAbbreviatedTaxInvoiceCommand compensateCommand;
    private ProcessedAbbreviatedTaxInvoice validInvoice;

    @BeforeEach
    void setUp() {
        validCommand = new ProcessAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
            "doc-1", "<xml>test</xml>", "ABR-001"
        );

        compensateCommand = new CompensateAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice"
        );

        Party seller = Party.of(
            "Seller", TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 St", "Bangkok", "10110", "TH")
        );
        LineItem item = new LineItem("Service", 10, Money.of(1000.00, "THB"), new BigDecimal("7.00"));

        validInvoice = ProcessedAbbreviatedTaxInvoice.builder()
            .id(AbbreviatedTaxInvoiceId.generate())
            .sourceInvoiceId("doc-1")
            .invoiceNumber("ABR-001")
            .issueDate(LocalDate.of(2025, 1, 1))
            .dueDate(LocalDate.of(2025, 2, 1))
            .seller(seller)
            .addItem(item)
            .currency("THB")
            .originalXml("<xml>test</xml>")
            .build();
    }

    @Test
    void testHandleProcessCommandSuccess() throws Exception {
        when(processingService.processInvoiceForSaga(anyString(), anyString(), anyString()))
            .thenReturn(validInvoice);

        handler.handleProcessCommand(validCommand);

        verify(processingService).processInvoiceForSaga("doc-1", "<xml>test</xml>", "corr-1");
        verify(sagaReplyPublisher).publishSuccess("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
        verify(sagaReplyPublisher, never()).publishFailure(any(), any(), any(), any());
    }

    @Test
    void testHandleProcessCommandFailure() throws Exception {
        when(processingService.processInvoiceForSaga(anyString(), anyString(), anyString()))
            .thenThrow(new AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException("Parse error"));

        handler.handleProcessCommand(validCommand);

        verify(sagaReplyPublisher).publishFailure("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1", "Parse error");
        verify(sagaReplyPublisher, never()).publishSuccess(any(), any(), any());
    }

    @Test
    void testHandleProcessCommandRuntimeError() throws Exception {
        when(processingService.processInvoiceForSaga(anyString(), anyString(), anyString()))
            .thenThrow(new RuntimeException("DB error"));

        handler.handleProcessCommand(validCommand);

        verify(sagaReplyPublisher).publishFailure("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1", "DB error");
        verify(sagaReplyPublisher, never()).publishSuccess(any(), any(), any());
    }

    @Test
    void testHandleCompensationFound() {
        when(invoiceRepository.findBySourceInvoiceId("doc-1")).thenReturn(Optional.of(validInvoice));

        handler.handleCompensation(compensateCommand);

        verify(invoiceRepository).deleteById(validInvoice.getId());
        verify(sagaReplyPublisher).publishCompensated("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
        verify(sagaReplyPublisher, never()).publishFailure(any(), any(), any(), any());
    }

    @Test
    void testHandleCompensationNotFound() {
        when(invoiceRepository.findBySourceInvoiceId("doc-1")).thenReturn(Optional.empty());

        handler.handleCompensation(compensateCommand);

        verify(invoiceRepository, never()).deleteById(any());
        verify(sagaReplyPublisher).publishCompensated("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
    }

    @Test
    void testHandleCompensationDeleteError() {
        when(invoiceRepository.findBySourceInvoiceId("doc-1")).thenReturn(Optional.of(validInvoice));
        doThrow(new RuntimeException("Delete failed")).when(invoiceRepository).deleteById(any());

        handler.handleCompensation(compensateCommand);

        verify(sagaReplyPublisher).publishFailure(
            eq("saga-1"), eq(SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE), eq("corr-1"),
            contains("Compensation failed")
        );
        verify(sagaReplyPublisher, never()).publishCompensated(any(), any(), any());
    }
}