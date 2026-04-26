package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.wpanther.saga.domain.enums.ReplyStatus;
import com.wpanther.saga.domain.enums.SagaStep;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbbreviatedTaxInvoiceReplyEventTest {

    @Test
    void testSuccessReply() {
        AbbreviatedTaxInvoiceReplyEvent reply = AbbreviatedTaxInvoiceReplyEvent.success(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"
        );

        assertTrue(reply.isSuccess());
        assertFalse(reply.isFailure());
        assertFalse(reply.isCompensated());
        assertEquals(ReplyStatus.SUCCESS, reply.getStatus());
        assertNull(reply.getErrorMessage());
        assertEquals("saga-1", reply.getSagaId());
        assertEquals(SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, reply.getSagaStep());
        assertEquals("corr-1", reply.getCorrelationId());
    }

    @Test
    void testFailureReply() {
        AbbreviatedTaxInvoiceReplyEvent reply = AbbreviatedTaxInvoiceReplyEvent.failure(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1", "Parse error"
        );

        assertFalse(reply.isSuccess());
        assertTrue(reply.isFailure());
        assertFalse(reply.isCompensated());
        assertEquals(ReplyStatus.FAILURE, reply.getStatus());
        assertEquals("Parse error", reply.getErrorMessage());
        assertEquals("saga-1", reply.getSagaId());
    }

    @Test
    void testCompensatedReply() {
        AbbreviatedTaxInvoiceReplyEvent reply = AbbreviatedTaxInvoiceReplyEvent.compensated(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"
        );

        assertFalse(reply.isSuccess());
        assertFalse(reply.isFailure());
        assertTrue(reply.isCompensated());
        assertEquals(ReplyStatus.COMPENSATED, reply.getStatus());
        assertNull(reply.getErrorMessage());
    }

    @Test
    void testInheritedFields() {
        AbbreviatedTaxInvoiceReplyEvent reply = AbbreviatedTaxInvoiceReplyEvent.success(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"
        );

        assertNotNull(reply.getEventId());
        assertNotNull(reply.getOccurredAt());
        assertEquals(1, reply.getVersion());
    }

    @Test
    void testDifferentSagaIds() {
        AbbreviatedTaxInvoiceReplyEvent reply1 = AbbreviatedTaxInvoiceReplyEvent.success(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1"
        );
        AbbreviatedTaxInvoiceReplyEvent reply2 = AbbreviatedTaxInvoiceReplyEvent.success(
            "saga-2", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-2"
        );

        assertNotEquals(reply1.getSagaId(), reply2.getSagaId());
        assertNotEquals(reply1.getCorrelationId(), reply2.getCorrelationId());
        assertNotEquals(reply1.getEventId(), reply2.getEventId());
    }
}