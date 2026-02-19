package com.wpanther.abbreviatedtaxinvoice.processing.domain.service;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;

/**
 * Domain service for parsing XML abbreviated tax invoices
 */
public interface AbbreviatedTaxInvoiceParserService {

    /**
     * Parse XML content into ProcessedAbbreviatedTaxInvoice domain model
     *
     * @param xmlContent The XML abbreviated tax invoice content
     * @param sourceInvoiceId The source invoice ID from intake service
     * @return Parsed abbreviated tax invoice domain model
     * @throws AbbreviatedTaxInvoiceParsingException if parsing fails
     */
    ProcessedAbbreviatedTaxInvoice parseInvoice(String xmlContent, String sourceInvoiceId)
            throws AbbreviatedTaxInvoiceParsingException;

    /**
     * Exception thrown when abbreviated tax invoice parsing fails
     */
    class AbbreviatedTaxInvoiceParsingException extends Exception {
        public AbbreviatedTaxInvoiceParsingException(String message) {
            super(message);
        }

        public AbbreviatedTaxInvoiceParsingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
