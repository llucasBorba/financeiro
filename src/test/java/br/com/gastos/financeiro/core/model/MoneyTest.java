package br.com.gastos.financeiro.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoneyTest {

    @Test
    @DisplayName("assume BRL quando a moeda não é informada")
    void assumeBrlAsDefaultCurrency() {
        assertEquals("BRL", new Money(BigDecimal.TEN, null).getCurrency());
    }

    @Test
    @DisplayName("rejeita valor nulo ou negativo")
    void rejectInvalidAmounts() {
        assertThrows(NullPointerException.class, () -> new Money(null, "BRL"));
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("-1.00"), "BRL"));
    }

    @Test
    @DisplayName("soma valores da mesma moeda")
    void addSameCurrency() {
        Money result = new Money(new BigDecimal("10.50"), "BRL").add(new Money(new BigDecimal("4.50"), "BRL"));

        assertEquals(0, new BigDecimal("15.00").compareTo(result.getAmount()));
        assertEquals("BRL", result.getCurrency());
    }

    @Test
    @DisplayName("impede operações entre moedas diferentes")
    void rejectDifferentCurrencies() {
        Money brl = new Money(BigDecimal.TEN, "BRL");
        Money usd = new Money(BigDecimal.ONE, "USD");

        assertThrows(IllegalArgumentException.class, () -> brl.add(usd));
        assertThrows(IllegalArgumentException.class, () -> brl.subtract(usd));
    }

    @Test
    @DisplayName("impede subtração que deixaria saldo negativo")
    void rejectInsufficientBalance() {
        Money saldo = new Money(new BigDecimal("10.00"), "BRL");
        Money saque = new Money(new BigDecimal("10.01"), "BRL");

        assertThrows(IllegalArgumentException.class, () -> saldo.subtract(saque));
    }

    @Test
    @DisplayName("mantém o contrato equals/hashCode para escalas diferentes")
    void equalsAndHashCodeAreConsistent() {
        Money a = new Money(new BigDecimal("10.0"), "BRL");
        Money b = new Money(new BigDecimal("10.00"), "BRL");

        assertEquals(a, b);
        assertEquals(a.hashCode(), b.hashCode());
    }

    @Test
    @DisplayName("reconhece valor zerado")
    void detectZero() {
        assertTrue(Money.zero("BRL").isZero());
    }
}
