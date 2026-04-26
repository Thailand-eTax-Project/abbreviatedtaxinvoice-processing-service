package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.Money;

import java.time.Instant;

public record AbbreviatedTaxInvoiceProcessedDomainEvent(
    String documentId,
    String documentNumber,
    Money total,
    String sagaId,
    String correlationId,
    Instant occurredAt
) {
    public static AbbreviatedTaxInvoiceProcessedDomainEvent of(
            String documentId,
            String documentNumber,
            Money total,
            String sagaId,
            String correlationId) {
        return new AbbreviatedTaxInvoiceProcessedDomainEvent(
            documentId, documentNumber, total, sagaId, correlationId, Instant.now());
    }
}
