package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for AbbreviatedTaxInvoiceId value object
 */
class AbbreviatedTaxInvoiceIdTest {

    @Test
    void testCreateInvoiceIdWithUUID() {
        UUID uuid = UUID.randomUUID();

        AbbreviatedTaxInvoiceId invoiceId = new AbbreviatedTaxInvoiceId(uuid);

        assertNotNull(invoiceId);
        assertEquals(uuid, invoiceId.value());
    }

    @Test
    void testGenerateInvoiceId() {
        AbbreviatedTaxInvoiceId invoiceId = AbbreviatedTaxInvoiceId.generate();

        assertNotNull(invoiceId);
        assertNotNull(invoiceId.value());
    }

    @Test
    void testGenerateMultipleInvoiceIds() {
        AbbreviatedTaxInvoiceId id1 = AbbreviatedTaxInvoiceId.generate();
        AbbreviatedTaxInvoiceId id2 = AbbreviatedTaxInvoiceId.generate();

        assertNotEquals(id1, id2);
        assertNotEquals(id1.value(), id2.value());
    }

    @Test
    void testCreateInvoiceIdFromString() {
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";

        AbbreviatedTaxInvoiceId invoiceId = AbbreviatedTaxInvoiceId.from(uuidString);

        assertNotNull(invoiceId);
        assertEquals(UUID.fromString(uuidString), invoiceId.value());
    }

    @Test
    void testNullUUID() {
        assertThrows(NullPointerException.class, () ->
            new AbbreviatedTaxInvoiceId(null)
        );
    }

    @Test
    void testFromNullString() {
        assertThrows(NullPointerException.class, () ->
            AbbreviatedTaxInvoiceId.from(null)
        );
    }

    @Test
    void testFromInvalidString() {
        String invalidUuid = "not-a-valid-uuid";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            AbbreviatedTaxInvoiceId.from(invalidUuid)
        );
        assertTrue(exception.getMessage().contains("Invalid abbreviated tax invoice ID format"));
    }

    @Test
    void testFromEmptyString() {
        String emptyString = "";

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () ->
            AbbreviatedTaxInvoiceId.from(emptyString)
        );
        assertTrue(exception.getMessage().contains("Invalid abbreviated tax invoice ID format"));
    }

    @Test
    void testToString() {
        String uuidString = "550e8400-e29b-41d4-a716-446655440000";
        AbbreviatedTaxInvoiceId invoiceId = AbbreviatedTaxInvoiceId.from(uuidString);

        String result = invoiceId.toString();

        assertEquals(uuidString, result);
    }

    @Test
    void testEquality() {
        UUID uuid = UUID.randomUUID();
        AbbreviatedTaxInvoiceId id1 = new AbbreviatedTaxInvoiceId(uuid);
        AbbreviatedTaxInvoiceId id2 = new AbbreviatedTaxInvoiceId(uuid);
        AbbreviatedTaxInvoiceId id3 = new AbbreviatedTaxInvoiceId(UUID.randomUUID());

        assertEquals(id1, id2);
        assertNotEquals(id1, id3);
    }

    @Test
    void testHashCode() {
        UUID uuid = UUID.randomUUID();
        AbbreviatedTaxInvoiceId id1 = new AbbreviatedTaxInvoiceId(uuid);
        AbbreviatedTaxInvoiceId id2 = new AbbreviatedTaxInvoiceId(uuid);

        assertEquals(id1.hashCode(), id2.hashCode());
    }

    @Test
    void testFromAndToStringRoundTrip() {
        AbbreviatedTaxInvoiceId original = AbbreviatedTaxInvoiceId.generate();
        String stringRepresentation = original.toString();

        AbbreviatedTaxInvoiceId reconstructed = AbbreviatedTaxInvoiceId.from(stringRepresentation);

        assertEquals(original, reconstructed);
        assertEquals(original.value(), reconstructed.value());
    }
}
