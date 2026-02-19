package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CompensateAbbreviatedTaxInvoiceCommandTest {

    @Test
    void testConvenienceConstructor() {
        CompensateAbbreviatedTaxInvoiceCommand cmd = new CompensateAbbreviatedTaxInvoiceCommand(
            "saga-1", "COMPENSATE_process-abbreviated-tax-invoice", "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice"
        );

        assertEquals("saga-1", cmd.getSagaId());
        assertEquals("COMPENSATE_process-abbreviated-tax-invoice", cmd.getSagaStep());
        assertEquals("corr-1", cmd.getCorrelationId());
        assertEquals("process-abbreviated-tax-invoice", cmd.getStepToCompensate());
        assertEquals("doc-1", cmd.getDocumentId());
        assertEquals("abbreviated-tax-invoice", cmd.getDocumentType());
        assertNotNull(cmd.getEventId());
        assertNotNull(cmd.getOccurredAt());
    }

    @Test
    void testFullConstructor() {
        UUID eventId = UUID.randomUUID();
        Instant occurredAt = Instant.now();

        CompensateAbbreviatedTaxInvoiceCommand cmd = new CompensateAbbreviatedTaxInvoiceCommand(
            eventId, occurredAt, "CompensationCommand", 1,
            "saga-1", "COMPENSATE_process-abbreviated-tax-invoice", "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice"
        );

        assertEquals(eventId, cmd.getEventId());
        assertEquals(occurredAt, cmd.getOccurredAt());
        assertEquals("CompensationCommand", cmd.getEventType());
        assertEquals("process-abbreviated-tax-invoice", cmd.getStepToCompensate());
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        CompensateAbbreviatedTaxInvoiceCommand original = new CompensateAbbreviatedTaxInvoiceCommand(
            "saga-1", "COMPENSATE_process-abbreviated-tax-invoice", "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice"
        );

        String json = objectMapper.writeValueAsString(original);
        CompensateAbbreviatedTaxInvoiceCommand deserialized =
            objectMapper.readValue(json, CompensateAbbreviatedTaxInvoiceCommand.class);

        assertEquals(original.getEventId(), deserialized.getEventId());
        assertEquals(original.getSagaId(), deserialized.getSagaId());
        assertEquals(original.getStepToCompensate(), deserialized.getStepToCompensate());
        assertEquals(original.getDocumentId(), deserialized.getDocumentId());
        assertEquals(original.getDocumentType(), deserialized.getDocumentType());
    }

    @Test
    void testEventType() {
        CompensateAbbreviatedTaxInvoiceCommand cmd = new CompensateAbbreviatedTaxInvoiceCommand(
            "saga-1", "COMPENSATE_process-abbreviated-tax-invoice", "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice"
        );

        assertEquals("CompensateAbbreviatedTaxInvoiceCommand", cmd.getEventType());
    }
}
