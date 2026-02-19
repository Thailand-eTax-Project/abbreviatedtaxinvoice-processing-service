package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/**
 * Value Object representing Abbreviated Tax Invoice identifier
 */
public record AbbreviatedTaxInvoiceId(UUID value) implements Serializable {

    public AbbreviatedTaxInvoiceId {
        Objects.requireNonNull(value, "Abbreviated Tax Invoice ID cannot be null");
    }

    /**
     * Generate a new unique abbreviated tax invoice ID
     */
    public static AbbreviatedTaxInvoiceId generate() {
        return new AbbreviatedTaxInvoiceId(UUID.randomUUID());
    }

    /**
     * Create abbreviated tax invoice ID from string
     */
    public static AbbreviatedTaxInvoiceId from(String id) {
        Objects.requireNonNull(id, "Abbreviated Tax Invoice ID string cannot be null");
        try {
            return new AbbreviatedTaxInvoiceId(UUID.fromString(id));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid abbreviated tax invoice ID format: " + id, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
