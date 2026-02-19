package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Address value object
 */
class AddressTest {

    @Test
    void testCreateAddressWithAllFields() {
        String streetAddress = "123 Business Street";
        String city = "Bangkok";
        String postalCode = "10110";
        String country = "TH";

        Address address = new Address(streetAddress, city, postalCode, country);

        assertNotNull(address);
        assertEquals(streetAddress, address.streetAddress());
        assertEquals(city, address.city());
        assertEquals(postalCode, address.postalCode());
        assertEquals(country, address.country());
    }

    @Test
    void testCreateAddressWithOfMethod() {
        Address address = Address.of("456 Street", "City", "12345", "US");

        assertNotNull(address);
        assertEquals("456 Street", address.streetAddress());
        assertEquals("City", address.city());
        assertEquals("12345", address.postalCode());
        assertEquals("US", address.country());
    }

    @Test
    void testCreateAddressWithNullStreetAddress() {
        Address address = new Address(null, "Bangkok", "10110", "TH");

        assertNotNull(address);
        assertNull(address.streetAddress());
    }

    @Test
    void testCreateAddressWithNullCity() {
        Address address = new Address("123 Street", null, "10110", "TH");

        assertNotNull(address);
        assertNull(address.city());
    }

    @Test
    void testCreateAddressWithNullPostalCode() {
        Address address = new Address("123 Street", "Bangkok", null, "TH");

        assertNotNull(address);
        assertNull(address.postalCode());
    }

    @Test
    void testNullCountry() {
        assertThrows(NullPointerException.class, () ->
            new Address("123 Street", "Bangkok", "10110", null)
        );
    }

    @Test
    void testBlankCountry() {
        assertThrows(IllegalArgumentException.class, () ->
            new Address("123 Street", "Bangkok", "10110", "")
        );

        assertThrows(IllegalArgumentException.class, () ->
            new Address("123 Street", "Bangkok", "10110", "   ")
        );
    }

    @Test
    void testToSingleLineWithAllFields() {
        Address address = new Address("123 Business Street", "Bangkok", "10110", "TH");

        String result = address.toSingleLine();

        assertEquals("123 Business Street, Bangkok 10110, TH", result);
    }

    @Test
    void testToSingleLineWithoutStreetAddress() {
        Address address = new Address(null, "Bangkok", "10110", "TH");

        String result = address.toSingleLine();

        assertEquals("Bangkok 10110, TH", result);
    }

    @Test
    void testToSingleLineWithOnlyCountry() {
        Address address = new Address(null, null, null, "TH");

        String result = address.toSingleLine();

        assertEquals("TH", result);
    }

    @Test
    void testEquality() {
        Address address1 = new Address("123 Street", "Bangkok", "10110", "TH");
        Address address2 = new Address("123 Street", "Bangkok", "10110", "TH");
        Address address3 = new Address("456 Street", "Bangkok", "10110", "TH");

        assertEquals(address1, address2);
        assertNotEquals(address1, address3);
    }

    @Test
    void testHashCode() {
        Address address1 = new Address("123 Street", "Bangkok", "10110", "TH");
        Address address2 = new Address("123 Street", "Bangkok", "10110", "TH");

        assertEquals(address1.hashCode(), address2.hashCode());
    }
}
