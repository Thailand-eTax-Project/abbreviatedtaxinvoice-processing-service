package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.service;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.service.AbbreviatedTaxInvoiceParserService;
import com.wpanther.etax.generated.abbreviatedtaxinvoice.ram.*;
import com.wpanther.etax.generated.abbreviatedtaxinvoice.rsm.AbbreviatedTaxInvoice_CrossIndustryInvoiceType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Implementation of AbbreviatedTaxInvoiceParserService that uses teda library's JAXB classes
 * to parse Thai e-Tax abbreviated tax invoice XML.
 *
 * Key differences from TaxInvoice parser:
 * - Uses abbreviatedtaxinvoice JAXB packages
 * - Abbreviated tax invoices have NO BuyerTradeParty
 * - TradeProductType.getName() returns a List<Max256TextType> (use .get(0).getValue())
 * - TradePartyType.getName() returns a singular Max256TextType (use .getValue())
 * - LineTradeAgreementType has GrossPriceProductTradePrice only (no NetPrice)
 */
@Slf4j
@Service
public class AbbreviatedTaxInvoiceParserServiceImpl implements AbbreviatedTaxInvoiceParserService {

    private final JAXBContext jaxbContext;

    public AbbreviatedTaxInvoiceParserServiceImpl() throws AbbreviatedTaxInvoiceParsingException {
        try {
            // Initialize JAXB context with the abbreviatedtaxinvoice implementation package
            // The teda library uses interface/implementation pattern with a custom JAXBContextFactory
            // CRITICAL: Uses abbreviatedtaxinvoice packages, NOT taxinvoice or invoice packages
            String contextPath = "com.wpanther.etax.generated.abbreviatedtaxinvoice.rsm.impl" +
                               ":com.wpanther.etax.generated.abbreviatedtaxinvoice.ram.impl" +
                               ":com.wpanther.etax.generated.common.qdt.impl" +
                               ":com.wpanther.etax.generated.common.udt.impl";
            this.jaxbContext = JAXBContext.newInstance(contextPath);
            log.info("JAXB context initialized successfully for Thai e-Tax abbreviated tax invoice parsing");
        } catch (JAXBException e) {
            log.error("Failed to initialize JAXB context", e);
            throw new AbbreviatedTaxInvoiceParsingException("Failed to initialize XML parser", e);
        }
    }

    @Override
    public ProcessedAbbreviatedTaxInvoice parseInvoice(String xmlContent, String sourceInvoiceId)
            throws AbbreviatedTaxInvoiceParsingException {

        log.debug("Starting XML parsing for source invoice ID: {}", sourceInvoiceId);

        try {
            // Step 1: Unmarshal XML to JAXB object
            AbbreviatedTaxInvoice_CrossIndustryInvoiceType jaxbInvoice = unmarshalXml(xmlContent);

            // Step 2: Extract invoice components
            ExchangedDocumentType document = jaxbInvoice.getExchangedDocument();
            if (document == null) {
                throw new AbbreviatedTaxInvoiceParsingException(
                    "Abbreviated tax invoice XML missing required ExchangedDocument element");
            }

            SupplyChainTradeTransactionType transaction = jaxbInvoice.getSupplyChainTradeTransaction();
            if (transaction == null) {
                throw new AbbreviatedTaxInvoiceParsingException(
                    "Abbreviated tax invoice XML missing required SupplyChainTradeTransaction element");
            }

            // Step 3: Map to domain model
            LocalDate issueDate = extractIssueDate(document);

            ProcessedAbbreviatedTaxInvoice invoice = ProcessedAbbreviatedTaxInvoice.builder()
                .id(AbbreviatedTaxInvoiceId.generate())
                .sourceInvoiceId(sourceInvoiceId)
                .invoiceNumber(extractInvoiceNumber(document))
                .issueDate(issueDate)
                .dueDate(extractDueDate(transaction, issueDate))
                .seller(extractSeller(transaction))
                // No buyer for abbreviated tax invoice
                .items(extractLineItems(transaction))
                .currency(extractCurrency(transaction))
                .originalXml(xmlContent)
                .build();

            log.info("Successfully parsed abbreviated tax invoice {} with {} line items",
                invoice.getInvoiceNumber(), invoice.getItems().size());

            return invoice;

        } catch (AbbreviatedTaxInvoiceParsingException e) {
            log.error("Failed to parse abbreviated tax invoice XML for source ID {}: {}",
                sourceInvoiceId, e.getMessage());
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error parsing abbreviated tax invoice XML for source ID " + sourceInvoiceId, e);
            throw new AbbreviatedTaxInvoiceParsingException(
                "Unexpected error during abbreviated tax invoice parsing", e);
        }
    }

    /**
     * Unmarshal XML string to JAXB object
     */
    private AbbreviatedTaxInvoice_CrossIndustryInvoiceType unmarshalXml(String xmlContent)
            throws AbbreviatedTaxInvoiceParsingException {

        if (xmlContent == null || xmlContent.isBlank()) {
            throw new AbbreviatedTaxInvoiceParsingException("XML content is null or empty");
        }

        try {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            StringReader reader = new StringReader(xmlContent);

            Object result = unmarshaller.unmarshal(reader);

            // Handle JAXBElement wrapper (common when no @XmlRootElement annotation)
            if (result instanceof jakarta.xml.bind.JAXBElement) {
                jakarta.xml.bind.JAXBElement<?> jaxbElement = (jakarta.xml.bind.JAXBElement<?>) result;
                result = jaxbElement.getValue();
            }

            if (!(result instanceof AbbreviatedTaxInvoice_CrossIndustryInvoiceType)) {
                throw new AbbreviatedTaxInvoiceParsingException(
                    "Unexpected root element: " + result.getClass().getName()
                );
            }

            return (AbbreviatedTaxInvoice_CrossIndustryInvoiceType) result;

        } catch (JAXBException e) {
            log.error("JAXB unmarshalling failed", e);
            throw new AbbreviatedTaxInvoiceParsingException("Failed to parse XML: " + e.getMessage(), e);
        }
    }

    /**
     * Extract invoice number from document
     */
    private String extractInvoiceNumber(ExchangedDocumentType document)
            throws AbbreviatedTaxInvoiceParsingException {

        if (document.getID() == null || document.getID().getValue() == null) {
            throw new AbbreviatedTaxInvoiceParsingException("Abbreviated tax invoice number (ID) is missing");
        }

        return document.getID().getValue();
    }

    /**
     * Extract issue date from document
     */
    private LocalDate extractIssueDate(ExchangedDocumentType document)
            throws AbbreviatedTaxInvoiceParsingException {

        XMLGregorianCalendar issueDateTime = document.getIssueDateTime();
        if (issueDateTime == null) {
            throw new AbbreviatedTaxInvoiceParsingException("Issue date/time is missing");
        }

        return convertXMLGregorianCalendarToLocalDate(issueDateTime);
    }

    /**
     * Extract due date from transaction settlement
     */
    private LocalDate extractDueDate(SupplyChainTradeTransactionType transaction, LocalDate issueDate)
            throws AbbreviatedTaxInvoiceParsingException {

        HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement == null) {
            throw new AbbreviatedTaxInvoiceParsingException("Trade settlement information is missing");
        }

        // Due date might be in payment terms
        List<TradePaymentTermsType> paymentTerms = settlement.getSpecifiedTradePaymentTerms();
        if (paymentTerms != null && !paymentTerms.isEmpty()) {
            TradePaymentTermsType terms = paymentTerms.get(0);
            XMLGregorianCalendar dueDateTime = terms.getDueDateDateTime();
            if (dueDateTime != null) {
                return convertXMLGregorianCalendarToLocalDate(dueDateTime);
            }
        }

        // Default to issue date + 30 days if not specified
        log.warn("Due date not found in XML, defaulting to issue date + 30 days");
        return issueDate.plusDays(30);
    }

    /**
     * Extract seller party information.
     * Abbreviated tax invoices have only SellerTradeParty (no BuyerTradeParty).
     */
    private Party extractSeller(SupplyChainTradeTransactionType transaction)
            throws AbbreviatedTaxInvoiceParsingException {

        HeaderTradeAgreementType agreement = transaction.getApplicableHeaderTradeAgreement();
        if (agreement == null || agreement.getSellerTradeParty() == null) {
            throw new AbbreviatedTaxInvoiceParsingException("Seller information is missing");
        }

        return mapParty(agreement.getSellerTradeParty(), "Seller");
    }

    /**
     * Map JAXB trade party to domain Party.
     * Note: TradePartyType.getName() returns singular Max256TextType (use .getValue())
     */
    private Party mapParty(TradePartyType jaxbParty, String partyType)
            throws AbbreviatedTaxInvoiceParsingException {

        // Extract name - abbreviated tax invoice TradePartyType.getName() returns SINGULAR Max256TextType
        String name = Optional.ofNullable(jaxbParty.getName())
            .map(n -> n.getValue())
            .orElseThrow(() -> new AbbreviatedTaxInvoiceParsingException(partyType + " name is missing"));

        // Extract tax identifier
        TaxIdentifier taxIdentifier = extractTaxIdentifier(jaxbParty, partyType);

        // Extract address
        Address address = extractAddress(jaxbParty, partyType);

        // Extract email (optional)
        String email = null;
        List<TradeContactType> contacts = jaxbParty.getDefinedTradeContact();
        if (contacts != null && !contacts.isEmpty()) {
            TradeContactType contact = contacts.get(0);
            if (contact.getEmailURIUniversalCommunication() != null &&
                contact.getEmailURIUniversalCommunication().getURIID() != null) {
                email = contact.getEmailURIUniversalCommunication().getURIID().getValue();
            }
        }

        return Party.of(name, taxIdentifier, address, email);
    }

    /**
     * Extract tax identifier from party.
     * TradePartyType.getSpecifiedTaxRegistration() returns singular TaxRegistrationType.
     */
    private TaxIdentifier extractTaxIdentifier(TradePartyType jaxbParty, String partyType)
            throws AbbreviatedTaxInvoiceParsingException {

        TaxRegistrationType taxReg = jaxbParty.getSpecifiedTaxRegistration();
        if (taxReg == null) {
            throw new AbbreviatedTaxInvoiceParsingException(partyType + " tax registration is missing");
        }

        if (taxReg.getID() == null || taxReg.getID().getValue() == null) {
            throw new AbbreviatedTaxInvoiceParsingException(partyType + " tax ID is missing");
        }

        String taxId = taxReg.getID().getValue();
        String scheme = Optional.ofNullable(taxReg.getID().getSchemeID())
            .orElse("VAT");

        return TaxIdentifier.of(taxId, scheme);
    }

    /**
     * Extract address from party
     */
    private Address extractAddress(TradePartyType jaxbParty, String partyType)
            throws AbbreviatedTaxInvoiceParsingException {

        TradeAddressType jaxbAddress = jaxbParty.getPostalTradeAddress();
        if (jaxbAddress == null) {
            throw new AbbreviatedTaxInvoiceParsingException(partyType + " address is missing");
        }

        // Build address (some fields may be optional)
        String streetAddress = Optional.ofNullable(jaxbAddress.getLineOne())
            .map(line -> line.getValue())
            .orElse(null);

        String city = Optional.ofNullable(jaxbAddress.getCityName())
            .map(n -> n.getValue())
            .orElse(null);

        String postalCode = Optional.ofNullable(jaxbAddress.getPostcodeCode())
            .map(code -> code.getValue())
            .orElse(null);

        String country = null;
        if (jaxbAddress.getCountryID() != null && jaxbAddress.getCountryID().getValue() != null) {
            country = jaxbAddress.getCountryID().getValue().value();
        }
        if (country == null) {
            throw new AbbreviatedTaxInvoiceParsingException(partyType + " country is missing");
        }

        return Address.of(streetAddress, city, postalCode, country);
    }

    /**
     * Extract line items
     */
    private List<LineItem> extractLineItems(SupplyChainTradeTransactionType transaction)
            throws AbbreviatedTaxInvoiceParsingException {

        List<SupplyChainTradeLineItemType> jaxbItems =
            transaction.getIncludedSupplyChainTradeLineItem();

        if (jaxbItems == null || jaxbItems.isEmpty()) {
            throw new AbbreviatedTaxInvoiceParsingException("Abbreviated tax invoice must have at least one line item");
        }

        List<LineItem> items = new ArrayList<>();
        String currency = extractCurrency(transaction);

        for (int i = 0; i < jaxbItems.size(); i++) {
            try {
                LineItem item = mapLineItem(jaxbItems.get(i), currency);
                items.add(item);
            } catch (Exception e) {
                throw new AbbreviatedTaxInvoiceParsingException(
                    "Failed to parse line item " + (i + 1) + ": " + e.getMessage(), e
                );
            }
        }

        return items;
    }

    /**
     * Map JAXB line item to domain LineItem.
     * Note: TradeProductType.getName() returns List<Max256TextType> (use .get(0).getValue())
     * Note: LineTradeAgreementType has GrossPriceProductTradePrice only (no NetPrice)
     */
    private LineItem mapLineItem(SupplyChainTradeLineItemType jaxbItem, String currency)
            throws AbbreviatedTaxInvoiceParsingException {

        // Extract product description
        // IMPORTANT: TradeProductType.getName() returns List<Max256TextType> for abbreviated tax invoice
        TradeProductType product = jaxbItem.getSpecifiedTradeProduct();
        if (product == null || product.getName() == null || product.getName().isEmpty()) {
            throw new AbbreviatedTaxInvoiceParsingException("Line item product name is missing");
        }
        String description = product.getName().get(0).getValue();

        // Extract quantity
        LineTradeDeliveryType delivery = jaxbItem.getSpecifiedLineTradeDelivery();
        if (delivery == null || delivery.getBilledQuantity() == null) {
            throw new AbbreviatedTaxInvoiceParsingException("Line item quantity is missing");
        }
        BigDecimal quantityDecimal = delivery.getBilledQuantity().getValue();
        int quantity = quantityDecimal.intValue();

        // Extract unit price - abbreviated tax invoice uses GrossPriceProductTradePrice only
        LineTradeAgreementType agreement = jaxbItem.getSpecifiedLineTradeAgreement();
        if (agreement == null || agreement.getGrossPriceProductTradePrice() == null) {
            throw new AbbreviatedTaxInvoiceParsingException("Line item unit price is missing");
        }
        TradePriceType priceType = agreement.getGrossPriceProductTradePrice();
        if (priceType.getChargeAmount() == null || priceType.getChargeAmount().isEmpty()) {
            throw new AbbreviatedTaxInvoiceParsingException("Line item price amount is missing");
        }
        BigDecimal unitPriceAmount = priceType.getChargeAmount().get(0).getValue();
        Money unitPrice = Money.of(unitPriceAmount, currency);

        // Extract tax rate
        LineTradeSettlementType settlement = jaxbItem.getSpecifiedLineTradeSettlement();
        BigDecimal taxRate = BigDecimal.ZERO;

        if (settlement != null && settlement.getApplicableTradeTax() != null
            && !settlement.getApplicableTradeTax().isEmpty()) {

            TradeTaxType tax = settlement.getApplicableTradeTax().get(0);
            if (tax.getCalculatedRate() != null) {
                taxRate = tax.getCalculatedRate();
            }
        }

        return new LineItem(description, quantity, unitPrice, taxRate);
    }

    /**
     * Extract currency code
     */
    private String extractCurrency(SupplyChainTradeTransactionType transaction)
            throws AbbreviatedTaxInvoiceParsingException {

        HeaderTradeSettlementType settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement == null || settlement.getInvoiceCurrencyCode() == null) {
            throw new AbbreviatedTaxInvoiceParsingException("Abbreviated tax invoice currency is missing");
        }

        String currency = null;
        if (settlement.getInvoiceCurrencyCode().getValue() != null) {
            currency = settlement.getInvoiceCurrencyCode().getValue().value();
        }

        if (currency == null || currency.length() != 3) {
            throw new AbbreviatedTaxInvoiceParsingException("Invalid currency code: " + currency);
        }

        return currency;
    }

    /**
     * Convert XMLGregorianCalendar to LocalDate
     */
    private LocalDate convertXMLGregorianCalendarToLocalDate(XMLGregorianCalendar calendar) {
        if (calendar == null) {
            return null;
        }
        return LocalDate.of(
            calendar.getYear(),
            calendar.getMonth(),
            calendar.getDay()
        );
    }
}
