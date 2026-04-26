package com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AbbreviatedTaxInvoiceParserPortTest {

    @Test
    void forEmpty_containsMessage() {
        var ex = AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.forEmpty();
        assertTrue(ex.getMessage().contains("null or empty"));
    }

    @Test
    void forOversized_containsByteInfo() {
        var ex = AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.forOversized(600, 512);
        assertTrue(ex.getMessage().contains("600"));
        assertTrue(ex.getMessage().contains("512"));
    }

    @Test
    void forTimeout_containsSeconds() {
        var ex = AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.forTimeout(10);
        assertTrue(ex.getMessage().contains("10"));
    }

    @Test
    void forInterrupted_hasMessage() {
        var ex = AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.forInterrupted();
        assertTrue(ex.getMessage().contains("interrupted"));
    }

    @Test
    void forUnmarshal_preservesCause() {
        RuntimeException cause = new RuntimeException("jaxb error");
        var ex = AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.forUnmarshal(cause);
        assertEquals(cause, ex.getCause());
        assertTrue(ex.getMessage().contains("jaxb error"));
    }

    @Test
    void forUnexpectedRootElement_containsClassName() {
        var ex = AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException
            .forUnexpectedRootElement("SomeType");
        assertTrue(ex.getMessage().contains("SomeType"));
    }

    @Test
    void constructorWithMessage_works() {
        var ex = new AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException("custom");
        assertEquals("custom", ex.getMessage());
    }

    @Test
    void constructorWithMessageAndCause_works() {
        Throwable cause = new IllegalStateException("root");
        var ex = new AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException("msg", cause);
        assertEquals("msg", ex.getMessage());
        assertEquals(cause, ex.getCause());
    }
}
