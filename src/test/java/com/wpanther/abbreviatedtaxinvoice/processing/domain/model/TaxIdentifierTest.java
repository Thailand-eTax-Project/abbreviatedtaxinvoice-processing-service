package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for TaxIdentifier value object
 */
class TaxIdentifierTest {

    @Test
    void testCreateTaxIdentifierWithBothParameters() {
        String value = "1234567890123";
        String scheme = "VAT";

        TaxIdentifier taxId = new TaxIdentifier(value, scheme);

        assertNotNull(taxId);
        assertEquals(value, taxId.value());
        assertEquals(scheme, taxId.scheme());
    }

    @Test
    void testCreateTaxIdentifierWithOfMethodDefaultScheme() {
        String value = "1234567890123";

        TaxIdentifier taxId = TaxIdentifier.of(value);

        assertNotNull(taxId);
        assertEquals(value, taxId.value());
        assertEquals("VAT", taxId.scheme());
    }

    @Test
    void testCreateTaxIdentifierWithOfMethodCustomScheme() {
        String value = "1234567890123";
        String scheme = "EIN";

        TaxIdentifier taxId = TaxIdentifier.of(value, scheme);

        assertNotNull(taxId);
        assertEquals(value, taxId.value());
        assertEquals(scheme, taxId.scheme());
    }

    @Test
    void testNullValue() {
        assertThrows(NullPointerException.class, () ->
            new TaxIdentifier(null, "VAT")
        );
    }

    @Test
    void testBlankValue() {
        assertThrows(IllegalArgumentException.class, () ->
            new TaxIdentifier("", "VAT")
        );

        assertThrows(IllegalArgumentException.class, () ->
            new TaxIdentifier("   ", "VAT")
        );
    }

    @Test
    void testNullScheme() {
        String value = "1234567890123";

        TaxIdentifier taxId = new TaxIdentifier(value, null);

        assertNotNull(taxId);
        assertEquals(value, taxId.value());
        assertNull(taxId.scheme());
    }

    @Test
    void testToStringWithScheme() {
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");

        String result = taxId.toString();

        assertEquals("VAT:1234567890123", result);
    }

    @Test
    void testToStringWithNullScheme() {
        TaxIdentifier taxId = new TaxIdentifier("1234567890123", null);

        String result = taxId.toString();

        assertEquals("1234567890123", result);
    }

    @Test
    void testEquality() {
        TaxIdentifier taxId1 = TaxIdentifier.of("1234567890123", "VAT");
        TaxIdentifier taxId2 = TaxIdentifier.of("1234567890123", "VAT");
        TaxIdentifier taxId3 = TaxIdentifier.of("9876543210987", "VAT");

        assertEquals(taxId1, taxId2);
        assertNotEquals(taxId1, taxId3);
    }

    @Test
    void testEqualityDifferentSchemes() {
        TaxIdentifier taxId1 = TaxIdentifier.of("1234567890123", "VAT");
        TaxIdentifier taxId2 = TaxIdentifier.of("1234567890123", "EIN");

        assertNotEquals(taxId1, taxId2);
    }

    @Test
    void testHashCode() {
        TaxIdentifier taxId1 = TaxIdentifier.of("1234567890123", "VAT");
        TaxIdentifier taxId2 = TaxIdentifier.of("1234567890123", "VAT");

        assertEquals(taxId1.hashCode(), taxId2.hashCode());
    }
}
