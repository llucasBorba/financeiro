package br.com.gastos.financeiro.core.model;

import java.math.BigDecimal;
import java.util.Objects;

public class Money {

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        this.amount = Objects.requireNonNull(amount, "O valor é obrigatório.");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("O valor não pode ser negativo.");
        }
        this.currency = currency != null ? currency : "BRL";
    }

    public static Money zero(String currency) {
        return new Money(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        validateCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money subtract(Money other) {
        validateCurrency(other);
        if (this.amount.compareTo(other.amount) < 0) {
            throw new IllegalArgumentException("Saldo insuficiente.");
        }
        return new Money(this.amount.subtract(other.amount), this.currency);
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    private void validateCurrency(Money other) {
        Objects.requireNonNull(other, "O valor da operação é obrigatório.");
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Moedas diferentes não podem ser operadas juntas.");
        }
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Money money = (Money) o;
        return amount.compareTo(money.amount) == 0 && Objects.equals(currency, money.currency);
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount.stripTrailingZeros(), currency);
    }

    @Override
    public String toString() {
        return currency + " " + amount;
    }
}
