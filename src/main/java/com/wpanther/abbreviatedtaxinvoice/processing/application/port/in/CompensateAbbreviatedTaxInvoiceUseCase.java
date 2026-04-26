package com.wpanther.abbreviatedtaxinvoice.processing.application.port.in;

import com.wpanther.saga.domain.enums.SagaStep;

public interface CompensateAbbreviatedTaxInvoiceUseCase {

    void compensate(String documentId, String sagaId, SagaStep sagaStep, String correlationId);

    class AbbreviatedTaxInvoiceCompensationException extends RuntimeException {
        public AbbreviatedTaxInvoiceCompensationException(String message) { super(message); }
        public AbbreviatedTaxInvoiceCompensationException(String message, Throwable cause) { super(message, cause); }
    }
}
