package com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;

public interface AbbreviatedTaxInvoiceParserPort {

    ProcessedAbbreviatedTaxInvoice parse(String xmlContent, String sourceInvoiceId)
        throws AbbreviatedTaxInvoiceParsingException;

    class AbbreviatedTaxInvoiceParsingException extends Exception {
        public AbbreviatedTaxInvoiceParsingException(String message) { super(message); }
        public AbbreviatedTaxInvoiceParsingException(String message, Throwable cause) { super(message, cause); }
        public static AbbreviatedTaxInvoiceParsingException forEmpty() {
            return new AbbreviatedTaxInvoiceParsingException("XML content is null or empty");
        }
        public static AbbreviatedTaxInvoiceParsingException forOversized(int byteSize, int limitBytes) {
            return new AbbreviatedTaxInvoiceParsingException(
                "XML payload too large: " + byteSize + " bytes (limit " + limitBytes + " bytes)");
        }
        public static AbbreviatedTaxInvoiceParsingException forTimeout(long timeoutSeconds) {
            return new AbbreviatedTaxInvoiceParsingException(
                "XML parsing timed out after " + timeoutSeconds + " s — possible malformed input");
        }
        public static AbbreviatedTaxInvoiceParsingException forInterrupted() {
            return new AbbreviatedTaxInvoiceParsingException("XML parsing was interrupted");
        }
        public static AbbreviatedTaxInvoiceParsingException forUnmarshal(Throwable cause) {
            return new AbbreviatedTaxInvoiceParsingException(
                "XML parsing failed: " + cause.getMessage(), cause);
        }
        public static AbbreviatedTaxInvoiceParsingException forUnexpectedRootElement(String className) {
            return new AbbreviatedTaxInvoiceParsingException(
                "Unexpected root element: " + className);
        }
    }
}
