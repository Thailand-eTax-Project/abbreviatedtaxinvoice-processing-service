package com.wpanther.abbreviatedtaxinvoice.processing.application.service;

import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.repository.ProcessedAbbreviatedTaxInvoiceRepository;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.service.AbbreviatedTaxInvoiceParserService;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging.EventPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AbbreviatedTaxInvoiceProcessingService (saga version)
 */
@ExtendWith(MockitoExtension.class)
class AbbreviatedTaxInvoiceProcessingServiceTest {

    @Mock
    private ProcessedAbbreviatedTaxInvoiceRepository invoiceRepository;

    @Mock
    private AbbreviatedTaxInvoiceParserService parserService;

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AbbreviatedTaxInvoiceProcessingService service;

    private ProcessedAbbreviatedTaxInvoice validInvoice;

    @BeforeEach
    void setUp() {
        Party seller = Party.of(
            "Seller Company",
            TaxIdentifier.of("1234567890", "VAT"),
            new Address("123 Street", "Bangkok", "10110", "TH")
        );

        LineItem item = new LineItem(
            "Service 1",
            10,
            Money.of(1000.00, "THB"),
            new BigDecimal("7.00")
        );

        validInvoice = ProcessedAbbreviatedTaxInvoice.builder()
            .id(AbbreviatedTaxInvoiceId.generate())
            .sourceInvoiceId("intake-123")
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
    void testProcessInvoiceForSagaSuccess() throws Exception {
        // Given
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserService.parseInvoice(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any(ProcessedAbbreviatedTaxInvoice.class))).thenReturn(validInvoice);

        // When
        ProcessedAbbreviatedTaxInvoice result = service.processInvoiceForSaga("intake-123", "<xml>test</xml>", "correlation-123");

        // Then
        assertNotNull(result);
        verify(invoiceRepository).findBySourceInvoiceId("intake-123");
        verify(parserService).parseInvoice("<xml>test</xml>", "intake-123");
        verify(invoiceRepository, times(2)).save(any(ProcessedAbbreviatedTaxInvoice.class));
        verify(eventPublisher).publishAbbreviatedTaxInvoiceProcessed(any(AbbreviatedTaxInvoiceProcessedEvent.class));
    }

    @Test
    void testProcessInvoiceForSagaAlreadyProcessed() throws Exception {
        // Given
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.of(validInvoice));

        // When
        ProcessedAbbreviatedTaxInvoice result = service.processInvoiceForSaga("intake-123", "<xml>test</xml>", "correlation-123");

        // Then
        assertNotNull(result);
        assertEquals(validInvoice, result);
        verify(invoiceRepository).findBySourceInvoiceId("intake-123");
        verify(parserService, never()).parseInvoice(anyString(), anyString());
        verify(invoiceRepository, never()).save(any(ProcessedAbbreviatedTaxInvoice.class));
        verify(eventPublisher, never()).publishAbbreviatedTaxInvoiceProcessed(any());
    }

    @Test
    void testProcessInvoiceForSagaParsingError() throws Exception {
        // Given
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserService.parseInvoice(anyString(), anyString()))
            .thenThrow(new AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException("Parse error"));

        // When / Then
        assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
            () -> service.processInvoiceForSaga("intake-123", "<xml>test</xml>", "correlation-123"));

        verify(parserService).parseInvoice("<xml>test</xml>", "intake-123");
        verify(invoiceRepository, never()).save(any(ProcessedAbbreviatedTaxInvoice.class));
        verify(eventPublisher, never()).publishAbbreviatedTaxInvoiceProcessed(any());
    }

    @Test
    void testProcessInvoiceForSagaPublishesCorrectEvent() throws Exception {
        // Given
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserService.parseInvoice(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any(ProcessedAbbreviatedTaxInvoice.class))).thenReturn(validInvoice);

        // When
        service.processInvoiceForSaga("intake-123", "<xml>test</xml>", "correlation-123");

        // Then
        ArgumentCaptor<AbbreviatedTaxInvoiceProcessedEvent> eventCaptor =
            ArgumentCaptor.forClass(AbbreviatedTaxInvoiceProcessedEvent.class);
        verify(eventPublisher).publishAbbreviatedTaxInvoiceProcessed(eventCaptor.capture());

        AbbreviatedTaxInvoiceProcessedEvent processedEvent = eventCaptor.getValue();
        assertEquals("ABR-001", processedEvent.getInvoiceNumber());
        assertEquals("THB", processedEvent.getCurrency());
        assertEquals("correlation-123", processedEvent.getCorrelationId());
    }

    @Test
    void testProcessInvoiceForSagaSavesTwice() throws Exception {
        // Given
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserService.parseInvoice(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any(ProcessedAbbreviatedTaxInvoice.class))).thenReturn(validInvoice);

        // When
        service.processInvoiceForSaga("intake-123", "<xml>test</xml>", "correlation-123");

        // Then - Should save twice: PROCESSING and COMPLETED
        verify(invoiceRepository, times(2)).save(any(ProcessedAbbreviatedTaxInvoice.class));
    }

    @Test
    void testProcessInvoiceForSagaDatabaseError() throws Exception {
        // Given
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserService.parseInvoice(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any(ProcessedAbbreviatedTaxInvoice.class)))
            .thenThrow(new RuntimeException("Database error"));

        // When / Then
        assertThrows(RuntimeException.class,
            () -> service.processInvoiceForSaga("intake-123", "<xml>test</xml>", "correlation-123"));

        verify(eventPublisher, never()).publishAbbreviatedTaxInvoiceProcessed(any());
    }

    @Test
    void testProcessInvoiceForSagaReturnsProcessedInvoice() throws Exception {
        // Given
        when(invoiceRepository.findBySourceInvoiceId(anyString())).thenReturn(Optional.empty());
        when(parserService.parseInvoice(anyString(), anyString())).thenReturn(validInvoice);
        when(invoiceRepository.save(any(ProcessedAbbreviatedTaxInvoice.class))).thenReturn(validInvoice);

        // When
        ProcessedAbbreviatedTaxInvoice result = service.processInvoiceForSaga("intake-123", "<xml>test</xml>", "corr-1");

        // Then
        assertNotNull(result);
        assertEquals("ABR-001", result.getInvoiceNumber());
    }

    @Test
    void testFindByIdValid() {
        // Given
        AbbreviatedTaxInvoiceId id = AbbreviatedTaxInvoiceId.generate();
        when(invoiceRepository.findById(any(AbbreviatedTaxInvoiceId.class))).thenReturn(Optional.of(validInvoice));

        // When
        Optional<ProcessedAbbreviatedTaxInvoice> result = service.findById(id.toString());

        // Then
        assertTrue(result.isPresent());
        assertEquals(validInvoice, result.get());
        verify(invoiceRepository).findById(any(AbbreviatedTaxInvoiceId.class));
    }

    @Test
    void testFindByIdInvalidFormat() {
        // Given
        String invalidId = "not-a-uuid";

        // When
        Optional<ProcessedAbbreviatedTaxInvoice> result = service.findById(invalidId);

        // Then
        assertFalse(result.isPresent());
        verify(invoiceRepository, never()).findById(any(AbbreviatedTaxInvoiceId.class));
    }

    @Test
    void testFindByIdNotFound() {
        // Given
        AbbreviatedTaxInvoiceId id = AbbreviatedTaxInvoiceId.generate();
        when(invoiceRepository.findById(any(AbbreviatedTaxInvoiceId.class))).thenReturn(Optional.empty());

        // When
        Optional<ProcessedAbbreviatedTaxInvoice> result = service.findById(id.toString());

        // Then
        assertFalse(result.isPresent());
        verify(invoiceRepository).findById(any(AbbreviatedTaxInvoiceId.class));
    }

    @Test
    void testFindByStatus() {
        // Given
        ProcessingStatus status = ProcessingStatus.COMPLETED;
        List<ProcessedAbbreviatedTaxInvoice> invoices = List.of(validInvoice);
        when(invoiceRepository.findByStatus(status)).thenReturn(invoices);

        // When
        List<ProcessedAbbreviatedTaxInvoice> result = service.findByStatus(status);

        // Then
        assertEquals(1, result.size());
        assertEquals(validInvoice, result.get(0));
        verify(invoiceRepository).findByStatus(status);
    }

    @Test
    void testFindByStatusEmpty() {
        // Given
        ProcessingStatus status = ProcessingStatus.FAILED;
        when(invoiceRepository.findByStatus(status)).thenReturn(List.of());

        // When
        List<ProcessedAbbreviatedTaxInvoice> result = service.findByStatus(status);

        // Then
        assertTrue(result.isEmpty());
        verify(invoiceRepository).findByStatus(status);
    }
}
