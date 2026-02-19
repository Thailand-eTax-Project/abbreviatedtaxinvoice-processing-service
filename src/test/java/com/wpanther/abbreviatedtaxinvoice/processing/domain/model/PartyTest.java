package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Party value object (abbreviated tax invoice - seller only)
 */
class PartyTest {

    @Test
    void testCreatePartyWithAllFields() {
        String name = "Acme Corporation Ltd.";
        TaxIdentifier taxId = TaxIdentifier.of("1234567890123", "VAT");
        Address address = new Address("123 Street", "Bangkok", "10110", "TH");
        String email = "info@acme.com";

        Party party = new Party(name, taxId, address, email);

        assertNotNull(party);
        assertEquals(name, party.name());
        assertEquals(taxId, party.taxIdentifier());
        assertEquals(address, party.address());
        assertEquals(email, party.email());
    }

    @Test
    void testCreatePartyWithOfMethodMinimal() {
        String name = "Test Company";
        TaxIdentifier taxId = TaxIdentifier.of("1234567890");
        Address address = new Address("Street", "City", "Code", "TH");

        Party party = Party.of(name, taxId, address);

        assertNotNull(party);
        assertEquals(name, party.name());
        assertEquals(taxId, party.taxIdentifier());
        assertEquals(address, party.address());
        assertNull(party.email());
    }

    @Test
    void testCreatePartyWithOfMethodWithEmail() {
        String name = "Test Company";
        TaxIdentifier taxId = TaxIdentifier.of("1234567890");
        Address address = new Address("Street", "City", "Code", "TH");
        String email = "test@company.com";

        Party party = Party.of(name, taxId, address, email);

        assertNotNull(party);
        assertEquals(name, party.name());
        assertEquals(email, party.email());
    }

    @Test
    void testNullName() {
        TaxIdentifier taxId = TaxIdentifier.of("1234567890");
        Address address = new Address("Street", "City", "Code", "TH");

        assertThrows(NullPointerException.class, () ->
            new Party(null, taxId, address, null)
        );
    }

    @Test
    void testBlankName() {
        TaxIdentifier taxId = TaxIdentifier.of("1234567890");
        Address address = new Address("Street", "City", "Code", "TH");

        assertThrows(IllegalArgumentException.class, () ->
            new Party("", taxId, address, null)
        );

        assertThrows(IllegalArgumentException.class, () ->
            new Party("   ", taxId, address, null)
        );
    }

    @Test
    void testNullTaxIdentifier() {
        String name = "Test Company";
        Address address = new Address("Street", "City", "Code", "TH");

        Party party = new Party(name, null, address, null);

        assertNotNull(party);
        assertNull(party.taxIdentifier());
    }

    @Test
    void testNullAddress() {
        String name = "Test Company";
        TaxIdentifier taxId = TaxIdentifier.of("1234567890");

        Party party = new Party(name, taxId, null, null);

        assertNotNull(party);
        assertNull(party.address());
    }

    @Test
    void testNullEmail() {
        String name = "Test Company";
        TaxIdentifier taxId = TaxIdentifier.of("1234567890");
        Address address = new Address("Street", "City", "Code", "TH");

        Party party = new Party(name, taxId, address, null);

        assertNotNull(party);
        assertNull(party.email());
        assertFalse(party.hasEmail());
    }

    @Test
    void testHasEmailWithValidEmail() {
        Party party = Party.of(
            "Test Company",
            TaxIdentifier.of("1234567890"),
            new Address("Street", "City", "Code", "TH"),
            "test@company.com"
        );

        assertTrue(party.hasEmail());
    }

    @Test
    void testHasEmailWithNullEmail() {
        Party party = Party.of(
            "Test Company",
            TaxIdentifier.of("1234567890"),
            new Address("Street", "City", "Code", "TH")
        );

        assertFalse(party.hasEmail());
    }

    @Test
    void testEquality() {
        TaxIdentifier taxId = TaxIdentifier.of("1234567890");
        Address address = new Address("Street", "City", "Code", "TH");

        Party party1 = new Party("Company A", taxId, address, "email@test.com");
        Party party2 = new Party("Company A", taxId, address, "email@test.com");
        Party party3 = new Party("Company B", taxId, address, "email@test.com");

        assertEquals(party1, party2);
        assertNotEquals(party1, party3);
    }

    @Test
    void testSellerScenario() {
        // Abbreviated tax invoices only have seller party
        Party seller = Party.of(
            "Seller Company",
            TaxIdentifier.of("1111111111", "VAT"),
            new Address("123 Seller St", "Bangkok", "10110", "TH"),
            "seller@company.com"
        );

        assertNotNull(seller);
        assertEquals("Seller Company", seller.name());
        assertEquals("1111111111", seller.taxIdentifier().value());
        assertEquals("VAT", seller.taxIdentifier().scheme());
        assertTrue(seller.hasEmail());
    }
}
