package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbbreviatedTaxInvoiceProcessedEvent (application layer Kafka DTO)
 */
class AbbreviatedTaxInvoiceProcessedEventTest {

    @Test
    void testCreateEvent() {
        String documentId = "abrinvoice-123";
        String invoiceNumber = "ABR-001";
        BigDecimal total = new BigDecimal("10000.00");
        String currency = "THB";
        String correlationId = "correlation-123";

        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            documentId, invoiceNumber, total, currency, correlationId
        );

        assertNotNull(event);
        assertEquals(documentId, event.getDocumentId());
        assertEquals(invoiceNumber, event.getInvoiceNumber());
        assertEquals(total, event.getTotal());
        assertEquals(currency, event.getCurrency());
        assertEquals(correlationId, event.getCorrelationId());
        assertEquals("abbreviated.taxinvoice.processed", event.getEventType());
        assertNotNull(event.getEventId());
        assertNotNull(event.getOccurredAt());
    }

    @Test
    void testJsonSerialization() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "abrinvoice-123",
            "ABR-001",
            new BigDecimal("10000.00"),
            "THB",
            "correlation-123"
        );

        String json = objectMapper.writeValueAsString(event);
        AbbreviatedTaxInvoiceProcessedEvent deserialized =
            objectMapper.readValue(json, AbbreviatedTaxInvoiceProcessedEvent.class);

        assertEquals(event.getEventId(), deserialized.getEventId());
        assertEquals(event.getDocumentId(), deserialized.getDocumentId());
        assertEquals(event.getInvoiceNumber(), deserialized.getInvoiceNumber());
        assertEquals(event.getTotal(), deserialized.getTotal());
        assertEquals(event.getCurrency(), deserialized.getCurrency());
        assertEquals(event.getCorrelationId(), deserialized.getCorrelationId());
    }
}