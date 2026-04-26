package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.ProcessAbbreviatedTaxInvoiceCommand;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProcessAbbreviatedTaxInvoiceCommandTest {

    @Test
    void testConvenienceConstructor() {
        ProcessAbbreviatedTaxInvoiceCommand cmd = new ProcessAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
            "doc-1", "<xml>test</xml>", "ABR-001"
        );

        assertEquals("saga-1", cmd.getSagaId());
        assertEquals(SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, cmd.getSagaStep());
        assertEquals("corr-1", cmd.getCorrelationId());
        assertEquals("doc-1", cmd.getDocumentId());
        assertEquals("<xml>test</xml>", cmd.getXmlContent());
        assertEquals("ABR-001", cmd.getInvoiceNumber());
        assertNotNull(cmd.getEventId());
        assertNotNull(cmd.getOccurredAt());
    }

    @Test
    void testJsonRoundTrip() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        ProcessAbbreviatedTaxInvoiceCommand original = new ProcessAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
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
}