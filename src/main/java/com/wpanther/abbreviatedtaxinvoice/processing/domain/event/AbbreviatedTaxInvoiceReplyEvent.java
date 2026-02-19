package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.wpanther.saga.domain.enums.ReplyStatus;
import com.wpanther.saga.domain.model.SagaReply;

/**
 * Concrete SagaReply for the abbreviatedtaxinvoice-processing-service.
 * Published to Kafka topic: saga.reply.abbreviated-tax-invoice
 */
public class AbbreviatedTaxInvoiceReplyEvent extends SagaReply {

    private static final long serialVersionUID = 1L;

    /**
     * Create a SUCCESS reply.
     */
    public static AbbreviatedTaxInvoiceReplyEvent success(String sagaId, String sagaStep, String correlationId) {
        return new AbbreviatedTaxInvoiceReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.SUCCESS);
    }

    /**
     * Create a FAILURE reply.
     */
    public static AbbreviatedTaxInvoiceReplyEvent failure(String sagaId, String sagaStep, String correlationId,
                                                           String errorMessage) {
        return new AbbreviatedTaxInvoiceReplyEvent(sagaId, sagaStep, correlationId, errorMessage);
    }

    /**
     * Create a COMPENSATED reply.
     */
    public static AbbreviatedTaxInvoiceReplyEvent compensated(String sagaId, String sagaStep, String correlationId) {
        return new AbbreviatedTaxInvoiceReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.COMPENSATED);
    }

    // For SUCCESS and COMPENSATED (delegates to SagaReply 4-arg status constructor)
    private AbbreviatedTaxInvoiceReplyEvent(String sagaId, String sagaStep, String correlationId, ReplyStatus status) {
        super(sagaId, sagaStep, correlationId, status);
    }

    // For FAILURE (delegates to SagaReply 4-arg error constructor)
    private AbbreviatedTaxInvoiceReplyEvent(String sagaId, String sagaStep, String correlationId, String errorMessage) {
        super(sagaId, sagaStep, correlationId, errorMessage);
    }
}
