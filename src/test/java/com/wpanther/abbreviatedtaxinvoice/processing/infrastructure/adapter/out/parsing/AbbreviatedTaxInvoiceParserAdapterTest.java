package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.parsing;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out.AbbreviatedTaxInvoiceParserPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbbreviatedTaxInvoiceParserAdapter — hexagonal out-bound adapter
 * with timeout, semaphore, and size guards.
 */
class AbbreviatedTaxInvoiceParserAdapterTest {

    private AbbreviatedTaxInvoiceParserPort parser;

    @BeforeEach
    void setUp() {
        parser = new AbbreviatedTaxInvoiceParserAdapter(10, 300);
    }

    // ------------------------------------------------------------------ happy path

    @Test
    void parse_validXml_returnsFullyPopulatedInvoice()
            throws AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException {

        String xml = sampleXml();
        ProcessedAbbreviatedTaxInvoice invoice = parser.parse(xml, "intake-001");

        assertNotNull(invoice);
        assertEquals("intake-001", invoice.getSourceInvoiceId());
        assertEquals("ABR2025-00001", invoice.getInvoiceNumber());
        assertEquals(LocalDate.of(2025, 1, 15), invoice.getIssueDate());
        assertEquals(LocalDate.of(2025, 2, 14), invoice.getDueDate());
        assertEquals("THB", invoice.getCurrency());
        assertEquals(xml, invoice.getOriginalXml());
    }

    @Test
    void parse_validXml_sellerInformationMapped()
            throws AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException {

        ProcessedAbbreviatedTaxInvoice invoice = parser.parse(sampleXml(), "src-1");

        Party seller = invoice.getSeller();
        assertNotNull(seller);
        assertEquals("Acme Corporation Ltd.", seller.name());
        assertEquals("1234567890123", seller.taxIdentifier().value());
        assertEquals("VAT", seller.taxIdentifier().scheme());

        Address addr = seller.address();
        assertNotNull(addr);
        assertEquals("123 Business Street", addr.streetAddress());
        assertEquals("Bangkok", addr.city());
        assertEquals("10110", addr.postalCode());
        assertEquals("TH", addr.country());
    }

    @Test
    void parse_validXml_lineItemsAndTotalsCorrect()
            throws AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException {

        ProcessedAbbreviatedTaxInvoice invoice = parser.parse(sampleXml(), "src-1");

        assertEquals(2, invoice.getItems().size());

        LineItem item1 = invoice.getItems().get(0);
        assertEquals("Professional Services - Consulting", item1.description());
        assertEquals(10, item1.quantity());
        assertEquals(Money.of(new BigDecimal("5000.00"), "THB"), item1.unitPrice());
        assertEquals(new BigDecimal("7.00"), item1.taxRate());

        LineItem item2 = invoice.getItems().get(1);
        assertEquals("Software License", item2.description());
        assertEquals(1, item2.quantity());
        assertEquals(Money.of(new BigDecimal("10000.00"), "THB"), item2.unitPrice());

        // Subtotal: (10 * 5000) + (1 * 10000) = 60,000
        assertEquals(Money.of(new BigDecimal("60000.00"), "THB"), invoice.getSubtotal());
        // Tax: 60,000 * 0.07 = 4,200
        assertEquals(Money.of(new BigDecimal("4200.00"), "THB"), invoice.getTotalTax());
        // Total: 64,200
        assertEquals(Money.of(new BigDecimal("64200.00"), "THB"), invoice.getTotal());
    }

    // ------------------------------------------------------------------ guards

    @Test
    void parse_nullContent_throwsForEmpty() {
        var ex = assertThrows(AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.class,
                () -> parser.parse(null, "src-1"));
        assertTrue(ex.getMessage().contains("null or empty"));
    }

    @Test
    void parse_blankContent_throwsForEmpty() {
        var ex = assertThrows(AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.class,
                () -> parser.parse("   ", "src-1"));
        assertTrue(ex.getMessage().contains("null or empty"));
    }

    @Test
    void parse_oversizedContent_throwsForOversized() {
        // Build a string larger than 512 KB
        StringBuilder sb = new StringBuilder("<?xml version=\"1.0\" encoding=\"UTF-8\"?><root>");
        int targetBytes = 513 * 1024; // 513 KB — just over limit
        while (sb.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length < targetBytes) {
            sb.append("<pad>").append("x".repeat(100)).append("</pad>");
        }
        sb.append("</root>");
        String oversized = sb.toString();

        var ex = assertThrows(AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.class,
                () -> parser.parse(oversized, "src-1"));
        assertTrue(ex.getMessage().contains("too large"));
    }

    @Test
    void parse_invalidXml_throwsForUnmarshal() {
        var ex = assertThrows(AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException.class,
                () -> parser.parse("<invalid>not an invoice</invalid>", "src-1"));
        assertTrue(ex.getMessage().contains("XML parsing failed"));
    }

    // ------------------------------------------------------------------ sample XML

    private String sampleXml() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16"
                xmlns:qdt="urn:etda:uncefact:data:standard:QualifiedDataType:1">

              <rsm:ExchangedDocumentContext>
                <ram:GuidelineSpecifiedDocumentContextParameter>
                  <ram:ID>urn:etda.or.th:abbreviatedtaxinvoice:2p1</ram:ID>
                </ram:GuidelineSpecifiedDocumentContextParameter>
              </rsm:ExchangedDocumentContext>

              <rsm:ExchangedDocument>
                <ram:ID>ABR2025-00001</ram:ID>
                <ram:TypeCode>388</ram:TypeCode>
                <ram:IssueDateTime>2025-01-15T00:00:00</ram:IssueDateTime>
              </rsm:ExchangedDocument>

              <rsm:SupplyChainTradeTransaction>
                <ram:ApplicableHeaderTradeAgreement>
                  <ram:SellerTradeParty>
                    <ram:Name>Acme Corporation Ltd.</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:LineOne>123 Business Street</ram:LineOne>
                      <ram:CityName>Bangkok</ram:CityName>
                      <ram:PostcodeCode>10110</ram:PostcodeCode>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID schemeID="VAT">1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>

                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                  <ram:SpecifiedTradePaymentTerms>
                    <ram:DueDateDateTime>2025-02-14T00:00:00</ram:DueDateDateTime>
                  </ram:SpecifiedTradePaymentTerms>
                </ram:ApplicableHeaderTradeSettlement>

                <ram:IncludedSupplyChainTradeLineItem>
                  <ram:AssociatedDocumentLineDocument>
                    <ram:LineID>1</ram:LineID>
                  </ram:AssociatedDocumentLineDocument>
                  <ram:SpecifiedTradeProduct>
                    <ram:Name>Professional Services - Consulting</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>5000.00</ram:ChargeAmount>
                    </ram:GrossPriceProductTradePrice>
                  </ram:SpecifiedLineTradeAgreement>
                  <ram:SpecifiedLineTradeDelivery>
                    <ram:BilledQuantity unitCode="HUR">10</ram:BilledQuantity>
                  </ram:SpecifiedLineTradeDelivery>
                  <ram:SpecifiedLineTradeSettlement>
                    <ram:ApplicableTradeTax>
                      <ram:TypeCode>VAT</ram:TypeCode>
                      <ram:CalculatedRate>7.00</ram:CalculatedRate>
                    </ram:ApplicableTradeTax>
                  </ram:SpecifiedLineTradeSettlement>
                </ram:IncludedSupplyChainTradeLineItem>

                <ram:IncludedSupplyChainTradeLineItem>
                  <ram:AssociatedDocumentLineDocument>
                    <ram:LineID>2</ram:LineID>
                  </ram:AssociatedDocumentLineDocument>
                  <ram:SpecifiedTradeProduct>
                    <ram:Name>Software License</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>10000.00</ram:ChargeAmount>
                    </ram:GrossPriceProductTradePrice>
                  </ram:SpecifiedLineTradeAgreement>
                  <ram:SpecifiedLineTradeDelivery>
                    <ram:BilledQuantity unitCode="C62">1</ram:BilledQuantity>
                  </ram:SpecifiedLineTradeDelivery>
                  <ram:SpecifiedLineTradeSettlement>
                    <ram:ApplicableTradeTax>
                      <ram:TypeCode>VAT</ram:TypeCode>
                      <ram:CalculatedRate>7.00</ram:CalculatedRate>
                    </ram:ApplicableTradeTax>
                  </ram:SpecifiedLineTradeSettlement>
                </ram:IncludedSupplyChainTradeLineItem>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }
}
