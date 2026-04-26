package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.CompensateAbbreviatedTaxInvoiceCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CompensateAbbreviatedTaxInvoiceCommandTest {

    @Test
    void testConvenienceConstructor() {
        CompensateAbbreviatedTaxInvoiceCommand cmd = new CompensateAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice"
        );

        assertEquals("saga-1", cmd.getSagaId());
        assertEquals(SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, cmd.getSagaStep());
        assertEquals("corr-1", cmd.getCorrelationId());
        assertEquals("process-abbreviated-tax-invoice", cmd.getStepToCompensate());
        assertEquals("doc-1", cmd.getDocumentId());
        assertEquals("abbreviated-tax-invoice", cmd.getDocumentType());
        assertNotNull(cmd.getEventId());
        assertNotNull(cmd.getOccurredAt());
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        CompensateAbbreviatedTaxInvoiceCommand original = new CompensateAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice"
        );

        String json = objectMapper.writeValueAsString(original);
        CompensateAbbreviatedTaxInvoiceCommand deserialized =
            objectMapper.readValue(json, CompensateAbbreviatedTaxInvoiceCommand.class);

        assertEquals(original.getEventId(), deserialized.getEventId());
        assertEquals(original.getSagaId(), deserialized.getSagaId());
        assertEquals(original.getSagaStep(), deserialized.getSagaStep());
        assertEquals(original.getStepToCompensate(), deserialized.getStepToCompensate());
        assertEquals(original.getDocumentId(), deserialized.getDocumentId());
        assertEquals(original.getDocumentType(), deserialized.getDocumentType());
    }
}