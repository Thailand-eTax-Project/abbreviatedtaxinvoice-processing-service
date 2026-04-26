package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.parsing;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out.AbbreviatedTaxInvoiceParserPort;
import com.wpanther.etax.generated.abbreviatedtaxinvoice.ram.*;
import com.wpanther.etax.generated.abbreviatedtaxinvoice.rsm.AbbreviatedTaxInvoice_CrossIndustryInvoiceType;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBElement;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Unmarshaller;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * Hexagonal adapter that implements the out-bound parser port with production-grade guards:
 * <ul>
 *   <li>Size guard — rejects payloads exceeding 512 KB</li>
 *   <li>Concurrency cap — semaphore limits concurrent JAXB unmarshal operations</li>
 *   <li>Timeout — wraps JAXB unmarshal in a FutureTask with configurable timeout</li>
 * </ul>
 */
@Slf4j
@Component
public class AbbreviatedTaxInvoiceParserAdapter implements AbbreviatedTaxInvoiceParserPort {

    private static final int MAX_BYTES = 512 * 1024; // 512 KB

    private final JAXBContext jaxbContext;
    private final Semaphore semaphore;
    private final ExecutorService executor;
    private final long timeoutSeconds;

    public AbbreviatedTaxInvoiceParserAdapter(
            @Value("${app.parsing.timeout-seconds:10}") long timeoutSeconds,
            @Value("${app.parsing.max-concurrent:300}") int maxConcurrent) {

        this.timeoutSeconds = timeoutSeconds;
        this.semaphore = new Semaphore(maxConcurrent);
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        try {
            String contextPath = "com.wpanther.etax.generated.abbreviatedtaxinvoice.rsm.impl"
                    + ":com.wpanther.etax.generated.abbreviatedtaxinvoice.ram.impl"
                    + ":com.wpanther.etax.generated.common.qdt.impl"
                    + ":com.wpanther.etax.generated.common.udt.impl";
            this.jaxbContext = JAXBContext.newInstance(contextPath);
            log.info("AbbreviatedTaxInvoiceParserAdapter initialised (timeout={}s, maxConcurrent={})",
                    timeoutSeconds, maxConcurrent);
        } catch (JAXBException e) {
            throw new IllegalStateException("Failed to initialise JAXB context for abbreviated tax invoice parsing", e);
        }
    }

    // ------------------------------------------------------------------ public API

    @Override
    public ProcessedAbbreviatedTaxInvoice parse(String xmlContent, String sourceInvoiceId)
            throws AbbreviatedTaxInvoiceParsingException {

        // 1. Empty / null guard
        if (xmlContent == null || xmlContent.isBlank()) {
            throw AbbreviatedTaxInvoiceParsingException.forEmpty();
        }

        // 2. Size guard (UTF-8 byte length)
        int byteSize = xmlContent.getBytes(StandardCharsets.UTF_8).length;
        if (byteSize > MAX_BYTES) {
            throw AbbreviatedTaxInvoiceParsingException.forOversized(byteSize, MAX_BYTES);
        }

        // 3. Acquire semaphore (concurrency cap)
        try {
            semaphore.acquire();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AbbreviatedTaxInvoiceParsingException.forInterrupted();
        }

        try {
            // 4. Unmarshal with timeout
            AbbreviatedTaxInvoice_CrossIndustryInvoiceType jaxbInvoice = unmarshalWithTimeout(xmlContent);

            // 5. Map to domain model
            return mapToDomain(jaxbInvoice, xmlContent, sourceInvoiceId);

        } finally {
            semaphore.release();
        }
    }

    // ------------------------------------------------------------------ unmarshal with timeout

    private AbbreviatedTaxInvoice_CrossIndustryInvoiceType unmarshalWithTimeout(String xmlContent)
            throws AbbreviatedTaxInvoiceParsingException {

        Callable<AbbreviatedTaxInvoice_CrossIndustryInvoiceType> task = () -> {
            Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
            Object result = unmarshaller.unmarshal(new StringReader(xmlContent));

            if (result instanceof JAXBElement<?> jaxbElement) {
                result = jaxbElement.getValue();
            }

            if (!(result instanceof AbbreviatedTaxInvoice_CrossIndustryInvoiceType)) {
                throw AbbreviatedTaxInvoiceParsingException.forUnexpectedRootElement(result.getClass().getName());
            }

            return (AbbreviatedTaxInvoice_CrossIndustryInvoiceType) result;
        };

        FutureTask<AbbreviatedTaxInvoice_CrossIndustryInvoiceType> futureTask = new FutureTask<>(task);
        executor.execute(futureTask);

        try {
            return futureTask.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            futureTask.cancel(true);
            throw AbbreviatedTaxInvoiceParsingException.forTimeout(timeoutSeconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw AbbreviatedTaxInvoiceParsingException.forInterrupted();
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof AbbreviatedTaxInvoiceParsingException parsing) {
                throw parsing;
            }
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(cause);
        }
    }

    // ------------------------------------------------------------------ domain mapping

    private ProcessedAbbreviatedTaxInvoice mapToDomain(
            AbbreviatedTaxInvoice_CrossIndustryInvoiceType jaxbInvoice,
            String xmlContent,
            String sourceInvoiceId)
            throws AbbreviatedTaxInvoiceParsingException {

        var document = jaxbInvoice.getExchangedDocument();
        if (document == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Missing ExchangedDocument element"));
        }

        var transaction = jaxbInvoice.getSupplyChainTradeTransaction();
        if (transaction == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Missing SupplyChainTradeTransaction element"));
        }

        LocalDate issueDate = extractIssueDate(document);

        return ProcessedAbbreviatedTaxInvoice.builder()
                .id(AbbreviatedTaxInvoiceId.generate())
                .sourceInvoiceId(sourceInvoiceId)
                .invoiceNumber(extractInvoiceNumber(document))
                .issueDate(issueDate)
                .dueDate(extractDueDate(transaction, issueDate))
                .seller(extractSeller(transaction))
                .items(extractLineItems(transaction))
                .currency(extractCurrency(transaction))
                .originalXml(xmlContent)
                .build();
    }

    // ------------------------------------------------------------------ extraction helpers

    private String extractInvoiceNumber(ExchangedDocumentType document)
            throws AbbreviatedTaxInvoiceParsingException {
        if (document.getID() == null || document.getID().getValue() == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Invoice number (ID) is missing"));
        }
        return document.getID().getValue();
    }

    private LocalDate extractIssueDate(ExchangedDocumentType document)
            throws AbbreviatedTaxInvoiceParsingException {
        XMLGregorianCalendar cal = document.getIssueDateTime();
        if (cal == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Issue date/time is missing"));
        }
        return toLocalDate(cal);
    }

    private LocalDate extractDueDate(SupplyChainTradeTransactionType transaction, LocalDate issueDate) {
        var settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement != null) {
            List<TradePaymentTermsType> terms = settlement.getSpecifiedTradePaymentTerms();
            if (terms != null && !terms.isEmpty()) {
                XMLGregorianCalendar dueCal = terms.get(0).getDueDateDateTime();
                if (dueCal != null) {
                    return toLocalDate(dueCal);
                }
            }
        }
        log.warn("Due date not found in XML, defaulting to issue date + 30 days");
        return issueDate.plusDays(30);
    }

    private Party extractSeller(SupplyChainTradeTransactionType transaction)
            throws AbbreviatedTaxInvoiceParsingException {
        var agreement = transaction.getApplicableHeaderTradeAgreement();
        if (agreement == null || agreement.getSellerTradeParty() == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Seller information is missing"));
        }
        return mapParty(agreement.getSellerTradeParty(), "Seller");
    }

    private Party mapParty(TradePartyType jaxbParty, String label)
            throws AbbreviatedTaxInvoiceParsingException {

        String name = Optional.ofNullable(jaxbParty.getName())
                .map(n -> n.getValue())
                .orElseThrow(() -> AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                        new IllegalStateException(label + " name is missing")));

        TaxIdentifier taxId = extractTaxIdentifier(jaxbParty, label);
        Address address = extractAddress(jaxbParty, label);

        String email = null;
        List<TradeContactType> contacts = jaxbParty.getDefinedTradeContact();
        if (contacts != null && !contacts.isEmpty()) {
            var contact = contacts.get(0);
            if (contact.getEmailURIUniversalCommunication() != null
                    && contact.getEmailURIUniversalCommunication().getURIID() != null) {
                email = contact.getEmailURIUniversalCommunication().getURIID().getValue();
            }
        }

        return Party.of(name, taxId, address, email);
    }

    private TaxIdentifier extractTaxIdentifier(TradePartyType jaxbParty, String label)
            throws AbbreviatedTaxInvoiceParsingException {
        TaxRegistrationType taxReg = jaxbParty.getSpecifiedTaxRegistration();
        if (taxReg == null || taxReg.getID() == null || taxReg.getID().getValue() == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException(label + " tax registration is missing"));
        }
        String taxIdValue = taxReg.getID().getValue();
        String scheme = Optional.ofNullable(taxReg.getID().getSchemeID()).orElse("VAT");
        return TaxIdentifier.of(taxIdValue, scheme);
    }

    private Address extractAddress(TradePartyType jaxbParty, String label)
            throws AbbreviatedTaxInvoiceParsingException {
        TradeAddressType addr = jaxbParty.getPostalTradeAddress();
        if (addr == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException(label + " address is missing"));
        }

        String street = Optional.ofNullable(addr.getLineOne()).map(l -> l.getValue()).orElse(null);
        String city = Optional.ofNullable(addr.getCityName()).map(n -> n.getValue()).orElse(null);
        String postalCode = Optional.ofNullable(addr.getPostcodeCode()).map(c -> c.getValue()).orElse(null);

        String country = null;
        if (addr.getCountryID() != null && addr.getCountryID().getValue() != null) {
            country = addr.getCountryID().getValue().value();
        }
        if (country == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException(label + " country is missing"));
        }

        return Address.of(street, city, postalCode, country);
    }

    private List<LineItem> extractLineItems(SupplyChainTradeTransactionType transaction)
            throws AbbreviatedTaxInvoiceParsingException {

        List<SupplyChainTradeLineItemType> jaxbItems = transaction.getIncludedSupplyChainTradeLineItem();
        if (jaxbItems == null || jaxbItems.isEmpty()) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("At least one line item is required"));
        }

        String currency = extractCurrency(transaction);
        List<LineItem> items = new ArrayList<>();

        for (int i = 0; i < jaxbItems.size(); i++) {
            try {
                items.add(mapLineItem(jaxbItems.get(i), currency));
            } catch (Exception e) {
                throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                        new IllegalStateException("Failed to parse line item " + (i + 1) + ": " + e.getMessage(), e));
            }
        }

        return items;
    }

    private LineItem mapLineItem(SupplyChainTradeLineItemType jaxbItem, String currency)
            throws AbbreviatedTaxInvoiceParsingException {

        // Product name — TradeProductType.getName() returns List<Max256TextType>
        TradeProductType product = jaxbItem.getSpecifiedTradeProduct();
        if (product == null || product.getName() == null || product.getName().isEmpty()) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Line item product name is missing"));
        }
        String description = product.getName().get(0).getValue();

        // Quantity
        var delivery = jaxbItem.getSpecifiedLineTradeDelivery();
        if (delivery == null || delivery.getBilledQuantity() == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Line item quantity is missing"));
        }
        int quantity = delivery.getBilledQuantity().getValue().intValue();

        // Unit price — abbreviated tax invoice uses GrossPriceProductTradePrice only
        var agreement = jaxbItem.getSpecifiedLineTradeAgreement();
        if (agreement == null || agreement.getGrossPriceProductTradePrice() == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Line item unit price is missing"));
        }
        TradePriceType priceType = agreement.getGrossPriceProductTradePrice();
        if (priceType.getChargeAmount() == null || priceType.getChargeAmount().isEmpty()) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Line item price amount is missing"));
        }
        BigDecimal unitPriceAmount = priceType.getChargeAmount().get(0).getValue();
        Money unitPrice = Money.of(unitPriceAmount, currency);

        // Tax rate
        var settlement = jaxbItem.getSpecifiedLineTradeSettlement();
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

    private String extractCurrency(SupplyChainTradeTransactionType transaction)
            throws AbbreviatedTaxInvoiceParsingException {
        var settlement = transaction.getApplicableHeaderTradeSettlement();
        if (settlement == null || settlement.getInvoiceCurrencyCode() == null) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Currency code is missing"));
        }
        String currency = null;
        if (settlement.getInvoiceCurrencyCode().getValue() != null) {
            currency = settlement.getInvoiceCurrencyCode().getValue().value();
        }
        if (currency == null || currency.length() != 3) {
            throw AbbreviatedTaxInvoiceParsingException.forUnmarshal(
                    new IllegalStateException("Invalid currency code: " + currency));
        }
        return currency;
    }

    private LocalDate toLocalDate(XMLGregorianCalendar cal) {
        return LocalDate.of(cal.getYear(), cal.getMonth(), cal.getDay());
    }
}
