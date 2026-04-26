package com.wpanther.abbreviatedtaxinvoice.processing.application.port.in;

import com.wpanther.saga.domain.enums.SagaStep;

public interface ProcessAbbreviatedTaxInvoiceUseCase {

    void process(String documentId, String xmlContent,
                 String sagaId, SagaStep sagaStep, String correlationId)
        throws AbbreviatedTaxInvoiceProcessingException;

    class AbbreviatedTaxInvoiceProcessingException extends Exception {
        public AbbreviatedTaxInvoiceProcessingException(String message) { super(message); }
        public AbbreviatedTaxInvoiceProcessingException(String message, Throwable cause) { super(message, cause); }
    }
}
