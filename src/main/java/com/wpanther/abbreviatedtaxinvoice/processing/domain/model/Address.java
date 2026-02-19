package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Value Object representing a postal address
 */
public record Address(
    String streetAddress,
    String city,
    String postalCode,
    String country
) implements Serializable {

    public Address {
        Objects.requireNonNull(country, "Country is required");
        if (country.isBlank()) {
            throw new IllegalArgumentException("Country cannot be blank");
        }
    }

    /**
     * Create address with all fields
     */
    public static Address of(String streetAddress, String city, String postalCode, String country) {
        return new Address(streetAddress, city, postalCode, country);
    }

    /**
     * Check if address has street
     */
    public boolean hasStreetAddress() {
        return streetAddress != null && !streetAddress.isBlank();
    }

    /**
     * Format address as a single line string
     */
    public String toSingleLine() {
        List<String> parts = new ArrayList<>();
        if (streetAddress != null && !streetAddress.isBlank()) {
            parts.add(streetAddress);
        }
        String cityPostal = buildCityPostal();
        if (!cityPostal.isEmpty()) {
            parts.add(cityPostal);
        }
        parts.add(country);
        return String.join(", ", parts);
    }

    private String buildCityPostal() {
        if (city != null && !city.isBlank() && postalCode != null && !postalCode.isBlank()) {
            return city + " " + postalCode;
        } else if (city != null && !city.isBlank()) {
            return city;
        } else if (postalCode != null && !postalCode.isBlank()) {
            return postalCode;
        }
        return "";
    }
}
