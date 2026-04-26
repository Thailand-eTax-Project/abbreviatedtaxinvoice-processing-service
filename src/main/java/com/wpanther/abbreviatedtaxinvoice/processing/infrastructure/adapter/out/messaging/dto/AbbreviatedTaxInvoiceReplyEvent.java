package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging.dto;

import com.wpanther.saga.domain.enums.ReplyStatus;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.SagaReply;

public class AbbreviatedTaxInvoiceReplyEvent extends SagaReply {

    private static final long serialVersionUID = 1L;

    public static AbbreviatedTaxInvoiceReplyEvent success(
            String sagaId, SagaStep sagaStep, String correlationId) {
        return new AbbreviatedTaxInvoiceReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.SUCCESS);
    }

    public static AbbreviatedTaxInvoiceReplyEvent failure(
            String sagaId, SagaStep sagaStep, String correlationId, String errorMessage) {
        return new AbbreviatedTaxInvoiceReplyEvent(sagaId, sagaStep, correlationId, errorMessage);
    }

    public static AbbreviatedTaxInvoiceReplyEvent compensated(
            String sagaId, SagaStep sagaStep, String correlationId) {
        return new AbbreviatedTaxInvoiceReplyEvent(sagaId, sagaStep, correlationId, ReplyStatus.COMPENSATED);
    }

    private AbbreviatedTaxInvoiceReplyEvent(
            String sagaId, SagaStep sagaStep, String correlationId, ReplyStatus status) {
        super(sagaId, sagaStep, correlationId, status);
    }

    private AbbreviatedTaxInvoiceReplyEvent(
            String sagaId, SagaStep sagaStep, String correlationId, String errorMessage) {
        super(sagaId, sagaStep, correlationId, errorMessage);
    }
}
