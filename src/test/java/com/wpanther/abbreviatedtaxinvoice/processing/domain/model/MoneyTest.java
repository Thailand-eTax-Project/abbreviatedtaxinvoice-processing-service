package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for Money value object
 */
class MoneyTest {

    @Test
    void testCreateMoneyWithBigDecimal() {
        BigDecimal amount = new BigDecimal("100.50");
        String currency = "THB";

        Money money = Money.of(amount, currency);

        assertNotNull(money);
        assertEquals(new BigDecimal("100.50"), money.amount());
        assertEquals("THB", money.currency());
    }

    @Test
    void testCreateMoneyWithDouble() {
        double amount = 100.50;
        String currency = "USD";

        Money money = Money.of(amount, currency);

        assertNotNull(money);
        assertEquals(new BigDecimal("100.50"), money.amount());
        assertEquals("USD", money.currency());
    }

    @Test
    void testCreateZeroMoney() {
        String currency = "EUR";

        Money money = Money.zero(currency);

        assertNotNull(money);
        assertEquals(BigDecimal.ZERO.setScale(2), money.amount());
        assertEquals("EUR", money.currency());
        assertTrue(money.isZero());
    }

    @Test
    void testAddMoney() {
        Money money1 = Money.of(100.50, "THB");
        Money money2 = Money.of(50.25, "THB");

        Money result = money1.add(money2);

        assertEquals(new BigDecimal("150.75"), result.amount());
        assertEquals("THB", result.currency());
    }

    @Test
    void testAddMoneyWithDifferentCurrency() {
        Money money1 = Money.of(100.00, "THB");
        Money money2 = Money.of(50.00, "USD");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> money1.add(money2));
        assertTrue(exception.getMessage().contains("Currency mismatch"));
    }

    @Test
    void testSubtractMoney() {
        Money money1 = Money.of(100.50, "THB");
        Money money2 = Money.of(50.25, "THB");

        Money result = money1.subtract(money2);

        assertEquals(new BigDecimal("50.25"), result.amount());
        assertEquals("THB", result.currency());
    }

    @Test
    void testSubtractMoneyWithDifferentCurrency() {
        Money money1 = Money.of(100.00, "THB");
        Money money2 = Money.of(50.00, "USD");

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> money1.subtract(money2));
        assertTrue(exception.getMessage().contains("Currency mismatch"));
    }

    @Test
    void testMultiplyByBigDecimal() {
        Money money = Money.of(100.00, "THB");
        BigDecimal factor = new BigDecimal("1.5");

        Money result = money.multiply(factor);

        assertEquals(new BigDecimal("150.00"), result.amount());
        assertEquals("THB", result.currency());
    }

    @Test
    void testMultiplyByDouble() {
        Money money = Money.of(100.00, "THB");
        double factor = 2.5;

        Money result = money.multiply(factor);

        assertEquals(new BigDecimal("250.00"), result.amount());
        assertEquals("THB", result.currency());
    }

    @Test
    void testMultiplyWithNull() {
        Money money = Money.of(100.00, "THB");

        assertThrows(NullPointerException.class, () -> money.multiply((BigDecimal) null));
    }

    @Test
    void testIsPositive() {
        Money positiveMoney = Money.of(100.00, "THB");
        Money negativeMoney = Money.of(-100.00, "THB");
        Money zeroMoney = Money.zero("THB");

        assertTrue(positiveMoney.isPositive());
        assertFalse(negativeMoney.isPositive());
        assertFalse(zeroMoney.isPositive());
    }

    @Test
    void testIsNegative() {
        Money positiveMoney = Money.of(100.00, "THB");
        Money negativeMoney = Money.of(-100.00, "THB");
        Money zeroMoney = Money.zero("THB");

        assertFalse(positiveMoney.isNegative());
        assertTrue(negativeMoney.isNegative());
        assertFalse(zeroMoney.isNegative());
    }

    @Test
    void testIsZero() {
        Money positiveMoney = Money.of(100.00, "THB");
        Money zeroMoney = Money.zero("THB");

        assertFalse(positiveMoney.isZero());
        assertTrue(zeroMoney.isZero());
    }

    @Test
    void testNullAmount() {
        assertThrows(NullPointerException.class, () -> Money.of(null, "THB"));
    }

    @Test
    void testNullCurrency() {
        assertThrows(NullPointerException.class, () -> Money.of(BigDecimal.TEN, null));
    }

    @Test
    void testInvalidCurrencyLength() {
        BigDecimal amount = BigDecimal.TEN;

        assertThrows(IllegalArgumentException.class, () -> Money.of(amount, "US"));
        assertThrows(IllegalArgumentException.class, () -> Money.of(amount, "USDT"));
    }

    @Test
    void testAmountScaleNormalization() {
        BigDecimal amount = new BigDecimal("100.123456");

        Money money = Money.of(amount, "THB");

        assertEquals(2, money.amount().scale());
        assertEquals(new BigDecimal("100.12"), money.amount());
    }

    @Test
    void testToString() {
        Money money = Money.of(100.50, "THB");

        String result = money.toString();

        assertEquals("100.50 THB", result);
    }

    @Test
    void testEquality() {
        Money money1 = Money.of(100.50, "THB");
        Money money2 = Money.of(100.50, "THB");
        Money money3 = Money.of(100.51, "THB");
        Money money4 = Money.of(100.50, "USD");

        assertEquals(money1, money2);
        assertNotEquals(money1, money3);
        assertNotEquals(money1, money4);
    }

    @Test
    void testHashCode() {
        Money money1 = Money.of(100.50, "THB");
        Money money2 = Money.of(100.50, "THB");

        assertEquals(money1.hashCode(), money2.hashCode());
    }
}
