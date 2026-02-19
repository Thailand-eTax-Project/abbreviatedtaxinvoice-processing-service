package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for LineItem value object
 */
class LineItemTest {

    @Test
    void testCreateValidLineItem() {
        String description = "Professional Services";
        int quantity = 10;
        Money unitPrice = Money.of(5000.00, "THB");
        BigDecimal taxRate = new BigDecimal("7.00");

        LineItem lineItem = new LineItem(description, quantity, unitPrice, taxRate);

        assertNotNull(lineItem);
        assertEquals(description, lineItem.description());
        assertEquals(quantity, lineItem.quantity());
        assertEquals(unitPrice, lineItem.unitPrice());
        assertEquals(taxRate, lineItem.taxRate());
    }

    @Test
    void testCalculateLineTotal() {
        LineItem lineItem = new LineItem(
            "Services", 10, Money.of(5000.00, "THB"), new BigDecimal("7.00")
        );

        Money lineTotal = lineItem.getLineTotal();

        assertEquals(Money.of(50000.00, "THB"), lineTotal);
    }

    @Test
    void testCalculateTaxAmount() {
        LineItem lineItem = new LineItem(
            "Services", 10, Money.of(5000.00, "THB"), new BigDecimal("7.00")
        );

        Money taxAmount = lineItem.getTaxAmount();

        // Tax: 50000 * 0.07 = 3500
        assertEquals(Money.of(3500.00, "THB"), taxAmount);
    }

    @Test
    void testCalculateTotalWithTax() {
        LineItem lineItem = new LineItem(
            "Services", 10, Money.of(5000.00, "THB"), new BigDecimal("7.00")
        );

        Money totalWithTax = lineItem.getTotalWithTax();

        // Total: 50000 + 3500 = 53500
        assertEquals(Money.of(53500.00, "THB"), totalWithTax);
    }

    @Test
    void testZeroTaxRate() {
        LineItem lineItem = new LineItem(
            "Services", 5, Money.of(1000.00, "THB"), BigDecimal.ZERO
        );

        Money taxAmount = lineItem.getTaxAmount();
        Money totalWithTax = lineItem.getTotalWithTax();

        assertEquals(Money.of(0.00, "THB"), taxAmount);
        assertEquals(Money.of(5000.00, "THB"), totalWithTax);
    }

    @Test
    void testNullDescription() {
        assertThrows(NullPointerException.class, () ->
            new LineItem(null, 10, Money.of(100.00, "THB"), new BigDecimal("7.00"))
        );
    }

    @Test
    void testBlankDescription() {
        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("", 10, Money.of(100.00, "THB"), new BigDecimal("7.00"))
        );

        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("   ", 10, Money.of(100.00, "THB"), new BigDecimal("7.00"))
        );
    }

    @Test
    void testNullUnitPrice() {
        assertThrows(NullPointerException.class, () ->
            new LineItem("Services", 10, null, new BigDecimal("7.00"))
        );
    }

    @Test
    void testNullTaxRate() {
        assertThrows(NullPointerException.class, () ->
            new LineItem("Services", 10, Money.of(100.00, "THB"), null)
        );
    }

    @Test
    void testZeroQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("Services", 0, Money.of(100.00, "THB"), new BigDecimal("7.00"))
        );
    }

    @Test
    void testNegativeQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("Services", -5, Money.of(100.00, "THB"), new BigDecimal("7.00"))
        );
    }

    @Test
    void testNegativeTaxRate() {
        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("Services", 10, Money.of(100.00, "THB"), new BigDecimal("-1.00"))
        );
    }

    @Test
    void testTaxRateAbove100() {
        assertThrows(IllegalArgumentException.class, () ->
            new LineItem("Services", 10, Money.of(100.00, "THB"), new BigDecimal("101.00"))
        );
    }

    @Test
    void testEquality() {
        LineItem item1 = new LineItem("Services", 10, Money.of(100.00, "THB"), new BigDecimal("7.00"));
        LineItem item2 = new LineItem("Services", 10, Money.of(100.00, "THB"), new BigDecimal("7.00"));
        LineItem item3 = new LineItem("Products", 10, Money.of(100.00, "THB"), new BigDecimal("7.00"));

        assertEquals(item1, item2);
        assertNotEquals(item1, item3);
    }

    @Test
    void testSingleQuantity() {
        LineItem lineItem = new LineItem(
            "Software License", 1, Money.of(10000.00, "THB"), new BigDecimal("7.00")
        );

        Money lineTotal = lineItem.getLineTotal();

        assertEquals(Money.of(10000.00, "THB"), lineTotal);
    }
}
