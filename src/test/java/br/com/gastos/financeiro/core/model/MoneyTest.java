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

    @Test
    @DisplayName("normaliza o código da moeda para caixa alta")
    void normalizesCurrencyCase() {
        assertEquals("BRL", new Money(BigDecimal.TEN, "brl").getCurrency());
        assertEquals("USD", new Money(BigDecimal.TEN, " usd ").getCurrency());
    }

    @Test
    @DisplayName("opera valores da mesma moeda escrita em caixas diferentes")
    void treatsCurrencyCaseInsensitively() {
        Money meta = new Money(new BigDecimal("100.00"), "brl");
        Money aporte = new Money(new BigDecimal("50.00"), "BRL");

        assertEquals(0, new BigDecimal("150.00").compareTo(meta.add(aporte).getAmount()));
    }

    @Test
    @DisplayName("rejeita código de moeda inexistente")
    void rejectsUnknownCurrency() {
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, "XYZ"));
        assertThrows(IllegalArgumentException.class, () -> new Money(BigDecimal.TEN, "reais"));
    }

    @Test
    @DisplayName("guarda toda quantia com duas casas decimais")
    void normalizesScale() {
        assertEquals("10.50", new Money(new BigDecimal("10.5"), "BRL").getAmount().toPlainString());
        assertEquals("10.00", new Money(BigDecimal.TEN, "BRL").getAmount().toPlainString());
        assertEquals("0.00", Money.zero("BRL").getAmount().toPlainString());
    }

    @Test
    @DisplayName("rejeita valor com mais casas decimais do que o banco guarda")
    void rejectsExcessivePrecision() {
        assertThrows(IllegalArgumentException.class, () -> new Money(new BigDecimal("250.755"), "BRL"));
    }

    @Test
    @DisplayName("recusa quantia acima do que NUMERIC(15,2) comporta")
    void rejeitaQuantiaAcimaDoTeto() {
        // Sem este teto, o primeiro a reclamar era o banco no INSERT: 500 em vez de 400.
        assertThrows(IllegalArgumentException.class,
                () -> new Money(new BigDecimal("99999999999999999.99"), "BRL"));
        assertThrows(IllegalArgumentException.class,
                () -> new Money(new BigDecimal("10000000000000.00"), "BRL"));
    }

    @Test
    @DisplayName("aceita exatamente o maior valor que a coluna comporta")
    void aceitaOTetoExato() {
        assertEquals(new BigDecimal("9999999999999.99"),
                new Money(Money.MAX_AMOUNT, "BRL").getAmount());
    }

    @Test
    @DisplayName("a soma que estoura o teto também é recusada")
    void somaQueEstouraOTeto() {
        Money quaseNoTeto = new Money(Money.MAX_AMOUNT, "BRL");
        assertThrows(IllegalArgumentException.class,
                () -> quaseNoTeto.add(new Money(new BigDecimal("0.01"), "BRL")));
    }
}
