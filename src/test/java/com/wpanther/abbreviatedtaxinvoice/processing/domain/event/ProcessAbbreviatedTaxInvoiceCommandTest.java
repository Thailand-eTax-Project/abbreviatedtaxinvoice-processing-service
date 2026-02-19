package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProcessAbbreviatedTaxInvoiceCommandTest {

    @Test
    void testConvenienceConstructor() {
        ProcessAbbreviatedTaxInvoiceCommand cmd = new ProcessAbbreviatedTaxInvoiceCommand(
            "saga-1", "process-abbreviated-tax-invoice", "corr-1",
            "doc-1", "<xml>test</xml>", "ABR-001"
        );

        assertEquals("saga-1", cmd.getSagaId());
        assertEquals("process-abbreviated-tax-invoice", cmd.getSagaStep());
        assertEquals("corr-1", cmd.getCorrelationId());
        assertEquals("doc-1", cmd.getDocumentId());
        assertEquals("<xml>test</xml>", cmd.getXmlContent());
        assertEquals("ABR-001", cmd.getInvoiceNumber());
        assertNotNull(cmd.getEventId());
        assertNotNull(cmd.getOccurredAt());
    }

    @Test
    void testFullConstructor() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        ProcessAbbreviatedTaxInvoiceCommand cmd = new ProcessAbbreviatedTaxInvoiceCommand(
            eventId, occurredAt, "ProcessAbbreviatedTaxInvoiceCommand", 1,
            "saga-1", "process-abbreviated-tax-invoice", "corr-1",
            "doc-1", "<xml>test</xml>", "ABR-001"
        );

        assertEquals(eventId, cmd.getEventId());
        assertEquals(occurredAt, cmd.getOccurredAt());
        assertEquals("ProcessAbbreviatedTaxInvoiceCommand", cmd.getEventType());
        assertEquals(1, cmd.getVersion());
        assertEquals("saga-1", cmd.getSagaId());
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        ProcessAbbreviatedTaxInvoiceCommand original = new ProcessAbbreviatedTaxInvoiceCommand(
            "saga-1", "process-abbreviated-tax-invoice", "corr-1",
            "doc-1", "<xml>test</xml>", "ABR-001"
        );

        String json = objectMapper.writeValueAsString(original);
        ProcessAbbreviatedTaxInvoiceCommand deserialized =
            objectMapper.readValue(json, ProcessAbbreviatedTaxInvoiceCommand.class);

        assertEquals(original.getEventId(), deserialized.getEventId());
        assertEquals(original.getSagaId(), deserialized.getSagaId());
        assertEquals(original.getSagaStep(), deserialized.getSagaStep());
        assertEquals(original.getCorrelationId(), deserialized.getCorrelationId());
        assertEquals(original.getDocumentId(), deserialized.getDocumentId());
        assertEquals(original.getXmlContent(), deserialized.getXmlContent());
        assertEquals(original.getInvoiceNumber(), deserialized.getInvoiceNumber());
    }

    @Test
    void testNullFields() {
        ProcessAbbreviatedTaxInvoiceCommand cmd = new ProcessAbbreviatedTaxInvoiceCommand(
            null, null, null, null, null, null
        );

        assertNull(cmd.getSagaId());
        assertNull(cmd.getDocumentId());
        assertNull(cmd.getXmlContent());
    }

    @Test
    void testEventType() {
        ProcessAbbreviatedTaxInvoiceCommand cmd = new ProcessAbbreviatedTaxInvoiceCommand(
            "saga-1", "process-abbreviated-tax-invoice", "corr-1",
            "doc-1", "<xml/>", "ABR-001"
        );

        assertEquals("ProcessAbbreviatedTaxInvoiceCommand", cmd.getEventType());
    }
}
