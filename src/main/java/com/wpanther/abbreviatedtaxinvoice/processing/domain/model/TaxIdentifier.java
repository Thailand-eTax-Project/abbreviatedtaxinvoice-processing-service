package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import java.io.Serializable;
import java.util.Objects;

/**
 * Value Object representing a tax identifier (e.g., VAT number)
 */
public record TaxIdentifier(
    String value,
    String scheme
) implements Serializable {

    public TaxIdentifier {
        Objects.requireNonNull(value, "Tax identifier value is required");

        if (value.isBlank()) {
            throw new IllegalArgumentException("Tax identifier value cannot be blank");
        }
    }

    /**
     * Create tax identifier with default VAT scheme
     */
    public static TaxIdentifier of(String value) {
        return new TaxIdentifier(value, "VAT");
    }

    /**
     * Create tax identifier with explicit scheme
     */
    public static TaxIdentifier of(String value, String scheme) {
        return new TaxIdentifier(value, scheme);
    }

    @Override
    public String toString() {
        if (scheme == null) {
            return value;
        }
        return String.format("%s:%s", scheme, value);
    }
}
