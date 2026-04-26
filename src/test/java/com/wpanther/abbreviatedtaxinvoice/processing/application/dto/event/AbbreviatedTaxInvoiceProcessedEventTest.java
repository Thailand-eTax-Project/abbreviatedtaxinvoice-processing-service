package com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class AbbreviatedTaxInvoiceProcessedEventTest {

    private final ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Test
    void convenienceConstructor_setsFields() {
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "doc-1", "INV-001", new BigDecimal("1500.00"), "THB", "corr-1");

        assertEquals("doc-1", event.getDocumentId());
        assertEquals("INV-001", event.getInvoiceNumber());
        assertEquals(new BigDecimal("1500.00"), event.getTotal());
        assertEquals("THB", event.getCurrency());
        assertEquals("corr-1", event.getCorrelationId());
    }

    @Test
    void sixArgConstructor_setsSagaId() {
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "doc-1", "INV-002", new BigDecimal("500.00"), "THB",
            "saga-1", "corr-2");

        assertEquals("saga-1", event.getSagaId());
        assertEquals("corr-2", event.getCorrelationId());
        assertEquals("doc-1", event.getDocumentId());
    }

    @Test
    void getEventType_returnsCorrectType() {
        AbbreviatedTaxInvoiceProcessedEvent event = new AbbreviatedTaxInvoiceProcessedEvent(
            "d", "i", BigDecimal.ONE, "THB", "c");

        assertEquals("abbreviated.taxinvoice.processed", event.getEventType());
    }

    @Test
    void jsonRoundTrip_preservesFields() throws Exception {
        AbbreviatedTaxInvoiceProcessedEvent original = new AbbreviatedTaxInvoiceProcessedEvent(
            "doc-1", "INV-003", new BigDecimal("2000.00"), "THB", "corr-3");

        String json = mapper.writeValueAsString(original);
        AbbreviatedTaxInvoiceProcessedEvent deserialized =
            mapper.readValue(json, AbbreviatedTaxInvoiceProcessedEvent.class);

        assertEquals(original.getDocumentId(), deserialized.getDocumentId());
        assertEquals(original.getInvoiceNumber(), deserialized.getInvoiceNumber());
        assertEquals(original.getTotal().stripTrailingZeros(), deserialized.getTotal().stripTrailingZeros());
        assertEquals(original.getCurrency(), deserialized.getCurrency());
        assertEquals(original.getCorrelationId(), deserialized.getCorrelationId());
    }
}
