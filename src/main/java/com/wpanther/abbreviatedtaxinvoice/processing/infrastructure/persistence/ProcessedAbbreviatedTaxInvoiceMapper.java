package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.persistence;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.*;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.persistence.AbbreviatedTaxInvoicePartyEntity.PartyType;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Mapper between domain model and JPA entities
 */
@Component
public class ProcessedAbbreviatedTaxInvoiceMapper {

    /**
     * Convert domain model to JPA entity
     */
    public ProcessedAbbreviatedTaxInvoiceEntity toEntity(ProcessedAbbreviatedTaxInvoice domain) {
        ProcessedAbbreviatedTaxInvoiceEntity entity = ProcessedAbbreviatedTaxInvoiceEntity.builder()
            .id(domain.getId().value())
            .sourceInvoiceId(domain.getSourceInvoiceId())
            .invoiceNumber(domain.getInvoiceNumber())
            .issueDate(domain.getIssueDate())
            .dueDate(domain.getDueDate())
            .currency(domain.getCurrency())
            .subtotal(domain.getSubtotal().amount())
            .totalTax(domain.getTotalTax().amount())
            .total(domain.getTotal().amount())
            .originalXml(domain.getOriginalXml())
            .status(domain.getStatus())
            .errorMessage(domain.getErrorMessage())
            .createdAt(domain.getCreatedAt())
            .completedAt(domain.getCompletedAt())
            .parties(new HashSet<>())
            .lineItems(new ArrayList<>())
            .build();

        // Map seller (abbreviated tax invoice has no buyer)
        AbbreviatedTaxInvoicePartyEntity seller = toPartyEntity(domain.getSeller(), PartyType.SELLER);
        entity.addParty(seller);

        // Map line items
        int lineNumber = 1;
        for (LineItem item : domain.getItems()) {
            AbbreviatedTaxInvoiceLineItemEntity lineItemEntity = toLineItemEntity(item, lineNumber++);
            entity.addLineItem(lineItemEntity);
        }

        return entity;
    }

    /**
     * Convert JPA entity to domain model
     */
    public ProcessedAbbreviatedTaxInvoice toDomain(ProcessedAbbreviatedTaxInvoiceEntity entity) {
        // Find seller party
        Party seller = null;

        for (AbbreviatedTaxInvoicePartyEntity partyEntity : entity.getParties()) {
            Party party = toPartyDomain(partyEntity);
            if (partyEntity.getPartyType() == PartyType.SELLER) {
                seller = party;
            }
        }

        // Convert line items
        List<LineItem> items = new ArrayList<>();
        for (AbbreviatedTaxInvoiceLineItemEntity itemEntity : entity.getLineItems()) {
            items.add(toLineItemDomain(itemEntity, entity.getCurrency()));
        }

        // Build domain object
        return ProcessedAbbreviatedTaxInvoice.builder()
            .id(AbbreviatedTaxInvoiceId.from(entity.getId().toString()))
            .sourceInvoiceId(entity.getSourceInvoiceId())
            .invoiceNumber(entity.getInvoiceNumber())
            .issueDate(entity.getIssueDate())
            .dueDate(entity.getDueDate())
            .seller(seller)
            .items(items)
            .currency(entity.getCurrency())
            .originalXml(entity.getOriginalXml())
            .status(entity.getStatus())
            .createdAt(entity.getCreatedAt())
            .completedAt(entity.getCompletedAt())
            .errorMessage(entity.getErrorMessage())
            .build();
    }

    private AbbreviatedTaxInvoicePartyEntity toPartyEntity(Party domain, PartyType partyType) {
        return AbbreviatedTaxInvoicePartyEntity.builder()
            .partyType(partyType)
            .name(domain.name())
            .taxId(domain.taxIdentifier() != null ? domain.taxIdentifier().value() : null)
            .taxIdScheme(domain.taxIdentifier() != null ? domain.taxIdentifier().scheme() : null)
            .streetAddress(domain.address() != null ? domain.address().streetAddress() : null)
            .city(domain.address() != null ? domain.address().city() : null)
            .postalCode(domain.address() != null ? domain.address().postalCode() : null)
            .country(domain.address() != null ? domain.address().country() : "UNKNOWN")
            .email(domain.email())
            .build();
    }

    private Party toPartyDomain(AbbreviatedTaxInvoicePartyEntity entity) {
        TaxIdentifier taxId = entity.getTaxId() != null
            ? TaxIdentifier.of(entity.getTaxId(), entity.getTaxIdScheme())
            : null;

        Address address = Address.of(
            entity.getStreetAddress(),
            entity.getCity(),
            entity.getPostalCode(),
            entity.getCountry()
        );

        return Party.of(entity.getName(), taxId, address, entity.getEmail());
    }

    private AbbreviatedTaxInvoiceLineItemEntity toLineItemEntity(LineItem domain, int lineNumber) {
        return AbbreviatedTaxInvoiceLineItemEntity.builder()
            .lineNumber(lineNumber)
            .description(domain.description())
            .quantity(domain.quantity())
            .unitPrice(domain.unitPrice().amount())
            .taxRate(domain.taxRate())
            .lineTotal(domain.getLineTotal().amount())
            .taxAmount(domain.getTaxAmount().amount())
            .build();
    }

    private LineItem toLineItemDomain(AbbreviatedTaxInvoiceLineItemEntity entity, String currency) {
        Money unitPrice = Money.of(entity.getUnitPrice(), currency);
        return new LineItem(
            entity.getDescription(),
            entity.getQuantity(),
            unitPrice,
            entity.getTaxRate()
        );
    }
}
