package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging;

import com.wpanther.saga.infrastructure.outbox.OutboxService;
import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.HeaderSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventPublisher
 */
@ExtendWith(MockitoExtension.class)
class EventPublisherTest {

    @Mock
    private OutboxService outboxService;

    @Mock
    private HeaderSerializer headerSerializer;

    private EventPublisher eventPublisher;

    @BeforeEach
    void setUp() {
        eventPublisher = new EventPublisher(outboxService, headerSerializer);
    }

    @Test
    void testPublishAbbreviatedTaxInvoiceProcessedSuccess() throws Exception {
        // Given
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "invoice-123",
            "ABR-001",
            new BigDecimal("10000.00"),
            "THB",
            "correlation-123"
        );

        when(headerSerializer.toJson(any())).thenReturn("{\"correlationId\":\"correlation-123\",\"invoiceNumber\":\"ABR-001\"}");

        // When
        eventPublisher.publishAbbreviatedTaxInvoiceProcessed(event);

        // Then
        verify(outboxService).saveWithRouting(
            eq(event),
            eq("ProcessedAbbreviatedTaxInvoice"),
            eq("invoice-123"),
            eq("abbreviated.taxinvoice.processed"),
            eq("invoice-123"),
            eq("{\"correlationId\":\"correlation-123\",\"invoiceNumber\":\"ABR-001\"}")
        );
    }

    @Test
    void testPublishAbbreviatedTaxInvoiceProcessedHeaderContent() throws Exception {
        // Given
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "invoice-123",
            "ABR-001",
            new BigDecimal("10000.00"),
            "THB",
            "correlation-123"
        );

        when(headerSerializer.toJson(any())).thenReturn("{\"correlationId\":\"correlation-123\",\"invoiceNumber\":\"ABR-001\"}");

        // When
        eventPublisher.publishAbbreviatedTaxInvoiceProcessed(event);

        // Then
        ArgumentCaptor<String> headersCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(
            any(), any(), any(), any(), any(),
            headersCaptor.capture()
        );

        String headers = headersCaptor.getValue();
        assertTrue(headers.contains("correlation-123"));
        assertTrue(headers.contains("ABR-001"));
    }

    @Test
    void testToJsonErrorThrowsIllegalStateException() throws Exception {
        // Given
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "invoice-123",
            "ABR-001",
            new BigDecimal("10000.00"),
            "THB",
            "correlation-123"
        );

        when(headerSerializer.toJson(any()))
            .thenThrow(new IllegalStateException("Failed to serialize"));

        // When / Then
        assertThrows(IllegalStateException.class,
            () -> eventPublisher.publishAbbreviatedTaxInvoiceProcessed(event));
    }

    @Test
    void testPublishUsesCorrectTopic() throws Exception {
        // Given
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "invoice-123",
            "ABR-001",
            new BigDecimal("10000.00"),
            "THB",
            "correlation-123"
        );

        when(headerSerializer.toJson(any())).thenReturn("{}");

        // When
        eventPublisher.publishAbbreviatedTaxInvoiceProcessed(event);

        // Then
        ArgumentCaptor<String> topicCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(any(), any(), any(), topicCaptor.capture(), any(), any());
        assertEquals("abbreviated.taxinvoice.processed", topicCaptor.getValue());
    }

    @Test
    void testPublishUsesInvoiceIdAsPartitionKey() throws Exception {
        // Given
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "invoice-456",
            "ABR-002",
            new BigDecimal("5000.00"),
            "THB",
            "correlation-456"
        );

        when(headerSerializer.toJson(any())).thenReturn("{}");

        // When
        eventPublisher.publishAbbreviatedTaxInvoiceProcessed(event);

        // Then
        ArgumentCaptor<String> partitionKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(outboxService).saveWithRouting(any(), any(), any(), any(), partitionKeyCaptor.capture(), any());
        assertEquals("invoice-456", partitionKeyCaptor.getValue());
    }
}