package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.service;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.service.AbbreviatedTaxInvoiceParserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbbreviatedTaxInvoiceParserServiceImpl
 */
class AbbreviatedTaxInvoiceParserServiceImplTest {

    private AbbreviatedTaxInvoiceParserService parserService;

    @BeforeEach
    void setUp() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        parserService = new AbbreviatedTaxInvoiceParserServiceImpl();
    }

    @Test
    void testParseValidAbbreviatedTaxInvoice() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: A valid Thai e-Tax abbreviated tax invoice XML
        String xmlContent = getSampleAbbreviatedTaxInvoiceXml();
        String sourceInvoiceId = "intake-12345";

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, sourceInvoiceId);

        // Then: All fields should be correctly parsed
        assertNotNull(invoice);
        assertEquals(sourceInvoiceId, invoice.getSourceInvoiceId());
        assertEquals("ABR2025-00001", invoice.getInvoiceNumber());
        assertEquals(LocalDate.of(2025, 1, 15), invoice.getIssueDate());
        assertEquals(LocalDate.of(2025, 2, 14), invoice.getDueDate());
        assertEquals("THB", invoice.getCurrency());
        assertNotNull(invoice.getId());
        assertEquals(xmlContent, invoice.getOriginalXml());
    }

    @Test
    void testParseSellerInformation() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: A valid abbreviated tax invoice XML
        String xmlContent = getSampleAbbreviatedTaxInvoiceXml();

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, "test-123");

        // Then: Seller information should be correctly parsed
        Party seller = invoice.getSeller();
        assertNotNull(seller);
        assertEquals("Acme Corporation Ltd.", seller.name());
        assertEquals("1234567890123", seller.taxIdentifier().value());
        assertEquals("VAT", seller.taxIdentifier().scheme());

        Address sellerAddress = seller.address();
        assertNotNull(sellerAddress);
        assertEquals("123 Business Street", sellerAddress.streetAddress());
        assertEquals("Bangkok", sellerAddress.city());
        assertEquals("10110", sellerAddress.postalCode());
        assertEquals("TH", sellerAddress.country());
    }

    @Test
    void testParseLineItems() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: A valid abbreviated tax invoice XML with line items
        String xmlContent = getSampleAbbreviatedTaxInvoiceXml();

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, "test-123");

        // Then: Line items should be correctly parsed
        assertNotNull(invoice.getItems());
        assertEquals(2, invoice.getItems().size());

        // First line item
        LineItem item1 = invoice.getItems().get(0);
        assertEquals("Professional Services - Consulting", item1.description());
        assertEquals(10, item1.quantity());
        assertEquals(Money.of(new BigDecimal("5000.00"), "THB"), item1.unitPrice());
        assertEquals(new BigDecimal("7.00"), item1.taxRate());

        // Second line item
        LineItem item2 = invoice.getItems().get(1);
        assertEquals("Software License", item2.description());
        assertEquals(1, item2.quantity());
        assertEquals(Money.of(new BigDecimal("10000.00"), "THB"), item2.unitPrice());
        assertEquals(new BigDecimal("7.00"), item2.taxRate());
    }

    @Test
    void testCalculateTotals() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: A valid abbreviated tax invoice XML
        String xmlContent = getSampleAbbreviatedTaxInvoiceXml();

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, "test-123");

        // Then: Totals should be calculated correctly
        // Subtotal: (10 * 5000) + (1 * 10000) = 60,000
        assertEquals(Money.of(new BigDecimal("60000.00"), "THB"), invoice.getSubtotal());

        // Tax: 60,000 * 0.07 = 4,200
        assertEquals(Money.of(new BigDecimal("4200.00"), "THB"), invoice.getTotalTax());

        // Total: 60,000 + 4,200 = 64,200
        assertEquals(Money.of(new BigDecimal("64200.00"), "THB"), invoice.getTotal());
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithNullXml() {
        // Given: Null XML content
        String xmlContent = null;

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
            () -> parserService.parseInvoice(xmlContent, "test-123"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithEmptyXml() {
        // Given: Empty XML content
        String xmlContent = "";

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
            () -> parserService.parseInvoice(xmlContent, "test-123"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithInvalidXml() {
        // Given: Invalid XML content
        String xmlContent = "<invalid>Not a valid abbreviated tax invoice</invalid>";

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
            () -> parserService.parseInvoice(xmlContent, "test-123"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingInvoiceNumber() {
        // Given: XML without invoice number
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutInvoiceNumber();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("invoice number"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingLineItems() {
        // Given: XML without line items
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutLineItems();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("line item"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingDueDate() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: XML without due date (should default to issue date + 30 days)
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutDueDate();

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, "test-123");

        // Then: Due date should be issue date + 30 days
        assertNotNull(invoice);
        assertEquals(LocalDate.of(2025, 1, 15), invoice.getIssueDate());
        assertEquals(LocalDate.of(2025, 2, 14), invoice.getDueDate());
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMinimalAddress() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: XML with minimal address (only country required)
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithMinimalAddress();

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, "test-123");

        // Then: Address should have only country
        assertNotNull(invoice);
        Party seller = invoice.getSeller();
        assertNotNull(seller.address());
        assertEquals("TH", seller.address().country());
        assertNull(seller.address().streetAddress());
        assertNull(seller.address().city());
        assertNull(seller.address().postalCode());
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithTaxIdNoScheme() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: XML with tax ID but no scheme (should default to "VAT")
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithTaxIdNoScheme();

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, "test-123");

        // Then: Tax scheme should default to "VAT"
        assertNotNull(invoice);
        assertEquals("VAT", invoice.getSeller().taxIdentifier().scheme());
        assertEquals("1234567890123", invoice.getSeller().taxIdentifier().value());
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingIssueDate() {
        // Given: XML without issue date
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutIssueDate();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Issue date"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingSeller() {
        // Given: XML without seller information
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutSeller();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Seller"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithInvalidCurrency() {
        // Given: XML with invalid currency code (not 3 characters)
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithInvalidCurrency();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("currency"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingCurrency() {
        // Given: XML without currency
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutCurrency();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("currency"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithLineItemNoTax() throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        // Given: XML with line item without tax info (should default to 0%)
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithLineItemNoTax();

        // When: Parsing the XML
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, "test-123");

        // Then: Tax rate should be zero
        assertNotNull(invoice);
        assertEquals(1, invoice.getItems().size());
        assertEquals(BigDecimal.ZERO, invoice.getItems().get(0).taxRate());
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingSellerName() {
        // Given: XML without seller name
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutSellerName();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Seller name"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingSellerTaxId() {
        // Given: XML without seller tax ID
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutSellerTaxId();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Seller tax"));
    }

    @Test
    void testParseAbbreviatedTaxInvoiceWithMissingSellerCountry() {
        // Given: XML without seller country
        String xmlContent = getAbbreviatedTaxInvoiceXmlWithoutSellerCountry();

        // When/Then: Should throw AbbreviatedTaxInvoiceParsingException
        AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException exception =
            assertThrows(AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException.class,
                () -> parserService.parseInvoice(xmlContent, "test-123"));

        assertTrue(exception.getMessage().contains("Seller country"));
    }

    /**
     * Sample Thai e-Tax abbreviated tax invoice XML for testing (no buyer party)
     */
    private String getSampleAbbreviatedTaxInvoiceXml() {
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

    private String getAbbreviatedTaxInvoiceXmlWithoutInvoiceNumber() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">

              <rsm:ExchangedDocumentContext>
                <ram:GuidelineSpecifiedDocumentContextParameter>
                  <ram:ID>urn:etda.or.th:abbreviatedtaxinvoice:2p1</ram:ID>
                </ram:GuidelineSpecifiedDocumentContextParameter>
              </rsm:ExchangedDocumentContext>

              <rsm:ExchangedDocument>
                <ram:TypeCode>388</ram:TypeCode>
                <ram:IssueDateTime>2025-01-15T00:00:00</ram:IssueDateTime>
              </rsm:ExchangedDocument>

              <rsm:SupplyChainTradeTransaction>
                <ram:ApplicableHeaderTradeAgreement>
                  <ram:SellerTradeParty>
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithoutLineItems() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">

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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithoutDueDate() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
                <ram:IncludedSupplyChainTradeLineItem>
                  <ram:AssociatedDocumentLineDocument>
                    <ram:LineID>1</ram:LineID>
                  </ram:AssociatedDocumentLineDocument>
                  <ram:SpecifiedTradeProduct>
                    <ram:Name>Test Product</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>1000.00</ram:ChargeAmount>
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

    private String getAbbreviatedTaxInvoiceXmlWithMinimalAddress() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
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
                    <ram:Name>Test Product</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>1000.00</ram:ChargeAmount>
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

    private String getAbbreviatedTaxInvoiceXmlWithTaxIdNoScheme() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
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
                    <ram:Name>Test Product</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>1000.00</ram:ChargeAmount>
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

    private String getAbbreviatedTaxInvoiceXmlWithoutIssueDate() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
              <rsm:ExchangedDocumentContext>
                <ram:GuidelineSpecifiedDocumentContextParameter>
                  <ram:ID>urn:etda.or.th:abbreviatedtaxinvoice:2p1</ram:ID>
                </ram:GuidelineSpecifiedDocumentContextParameter>
              </rsm:ExchangedDocumentContext>
              <rsm:ExchangedDocument>
                <ram:ID>ABR2025-00001</ram:ID>
                <ram:TypeCode>388</ram:TypeCode>
              </rsm:ExchangedDocument>
              <rsm:SupplyChainTradeTransaction>
                <ram:ApplicableHeaderTradeAgreement>
                  <ram:SellerTradeParty>
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithoutSeller() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithInvalidCurrency() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>INVALID</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
                <ram:IncludedSupplyChainTradeLineItem>
                  <ram:AssociatedDocumentLineDocument>
                    <ram:LineID>1</ram:LineID>
                  </ram:AssociatedDocumentLineDocument>
                  <ram:SpecifiedTradeProduct>
                    <ram:Name>Test Product</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                    </ram:GrossPriceProductTradePrice>
                  </ram:SpecifiedLineTradeAgreement>
                  <ram:SpecifiedLineTradeDelivery>
                    <ram:BilledQuantity unitCode="C62">1</ram:BilledQuantity>
                  </ram:SpecifiedLineTradeDelivery>
                </ram:IncludedSupplyChainTradeLineItem>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithoutCurrency() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                </ram:ApplicableHeaderTradeSettlement>
                <ram:IncludedSupplyChainTradeLineItem>
                  <ram:AssociatedDocumentLineDocument>
                    <ram:LineID>1</ram:LineID>
                  </ram:AssociatedDocumentLineDocument>
                  <ram:SpecifiedTradeProduct>
                    <ram:Name>Test Product</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                    </ram:GrossPriceProductTradePrice>
                  </ram:SpecifiedLineTradeAgreement>
                  <ram:SpecifiedLineTradeDelivery>
                    <ram:BilledQuantity unitCode="C62">1</ram:BilledQuantity>
                  </ram:SpecifiedLineTradeDelivery>
                </ram:IncludedSupplyChainTradeLineItem>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithLineItemNoTax() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
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
                    <ram:Name>Test Product</ram:Name>
                  </ram:SpecifiedTradeProduct>
                  <ram:SpecifiedLineTradeAgreement>
                    <ram:GrossPriceProductTradePrice>
                      <ram:ChargeAmount>1000.00</ram:ChargeAmount>
                    </ram:GrossPriceProductTradePrice>
                  </ram:SpecifiedLineTradeAgreement>
                  <ram:SpecifiedLineTradeDelivery>
                    <ram:BilledQuantity unitCode="C62">1</ram:BilledQuantity>
                  </ram:SpecifiedLineTradeDelivery>
                </ram:IncludedSupplyChainTradeLineItem>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithoutSellerName() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithoutSellerTaxId() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                      <ram:CountryID>TH</ram:CountryID>
                    </ram:PostalTradeAddress>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }

    private String getAbbreviatedTaxInvoiceXmlWithoutSellerCountry() {
        return """
            <?xml version="1.0" encoding="UTF-8"?>
            <rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice
                xmlns:rsm="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_CrossIndustryInvoice:2"
                xmlns:ram="urn:etda:uncefact:data:standard:AbbreviatedTaxInvoice_ReusableAggregateBusinessInformationEntity:2"
                xmlns:udt="urn:un:unece:uncefact:data:standard:UnqualifiedDataType:16">
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
                    <ram:Name>Test Seller</ram:Name>
                    <ram:PostalTradeAddress>
                    </ram:PostalTradeAddress>
                    <ram:SpecifiedTaxRegistration>
                      <ram:ID>1234567890123</ram:ID>
                    </ram:SpecifiedTaxRegistration>
                  </ram:SellerTradeParty>
                </ram:ApplicableHeaderTradeAgreement>
                <ram:ApplicableHeaderTradeSettlement>
                  <ram:InvoiceCurrencyCode>THB</ram:InvoiceCurrencyCode>
                </ram:ApplicableHeaderTradeSettlement>
              </rsm:SupplyChainTradeTransaction>
            </rsm:AbbreviatedTaxInvoice_CrossIndustryInvoice>
            """;
    }
}
