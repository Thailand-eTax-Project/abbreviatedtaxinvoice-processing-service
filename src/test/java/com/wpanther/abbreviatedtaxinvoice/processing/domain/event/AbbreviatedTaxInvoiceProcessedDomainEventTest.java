package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.Money;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class AbbreviatedTaxInvoiceProcessedDomainEventTest {

    @Test
    void of_setsAllFieldsAndStampsOccurredAt() {
        Money total = Money.of(new BigDecimal("1000.00"), "THB");
        Instant before = Instant.now();

        AbbreviatedTaxInvoiceProcessedDomainEvent event =
            AbbreviatedTaxInvoiceProcessedDomainEvent.of(
                "doc-123", "INV-001", total, "saga-1", "corr-1");

        Instant after = Instant.now();
        assertEquals("doc-123", event.documentId());
        assertEquals("INV-001", event.documentNumber());
        assertEquals(total, event.total());
        assertEquals("saga-1", event.sagaId());
        assertEquals("corr-1", event.correlationId());
        assertFalse(event.occurredAt().isBefore(before));
        assertFalse(event.occurredAt().isAfter(after));
    }

    @Test
    void canonicalConstructor_acceptsFixedTimestamp() {
        Money total = Money.of(new BigDecimal("500.00"), "THB");
        Instant fixed = Instant.parse("2026-01-01T00:00:00Z");

        AbbreviatedTaxInvoiceProcessedDomainEvent event =
            new AbbreviatedTaxInvoiceProcessedDomainEvent(
                "d", "N", total, "s", "c", fixed);

        assertEquals(fixed, event.occurredAt());
    }
}