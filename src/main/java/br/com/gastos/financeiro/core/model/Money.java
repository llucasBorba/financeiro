package br.com.gastos.financeiro.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * Value Object que representa uma quantia monetária.
 *
 * <p>A classe é imutável e normaliza a si mesma no construtor: se um {@code Money}
 * existe, ele é uma quantia válida. Isso evita espalhar validação de dinheiro pelos
 * serviços e garante que dois valores equivalentes sejam sempre representados do mesmo jeito.
 */
public class Money {

    /** Moeda assumida quando o cliente não informa nenhuma. */
    public static final String DEFAULT_CURRENCY = "BRL";

    /**
     * Casas decimais em que toda quantia é guardada.
     *
     * <p>Precisa acompanhar a escala das colunas {@code NUMERIC(15,2)} do banco: se o
     * domínio aceitasse mais precisão que a coluna, o banco arredondaria em silêncio e o
     * valor lido de volta não seria o valor gravado.
     */
    public static final int SCALE = 2;

    private final BigDecimal amount;
    private final String currency;

    public Money(BigDecimal amount, String currency) {
        Objects.requireNonNull(amount, "O valor é obrigatório.");
        if (amount.signum() < 0) {
            throw new IllegalArgumentException("O valor não pode ser negativo.");
        }
        this.amount = normalizeScale(amount);
        this.currency = normalizeCurrency(currency);
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

    /**
     * Ajusta a quantia para {@link #SCALE} casas decimais sem arredondar.
     *
     * <p>{@link RoundingMode#UNNECESSARY} significa "complete com zeros se faltar, mas
     * estoure se for preciso jogar fora algum dígito": {@code 10.5} vira {@code 10.50},
     * enquanto {@code 10.555} é rejeitado em vez de virar {@code 10.56} sem o usuário saber.
     */
    private static BigDecimal normalizeScale(BigDecimal amount) {
        try {
            return amount.setScale(SCALE, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException(
                    "O valor deve ter no máximo " + SCALE + " casas decimais: " + amount.toPlainString());
        }
    }

    /**
     * Coloca o código da moeda em caixa alta e confirma que ele existe na ISO 4217.
     *
     * <p>Sem isso, {@code "brl"} e {@code "BRL"} seriam moedas diferentes para o
     * {@link #validateCurrency(Money)} e um aporte legítimo seria recusado.
     */
    private static String normalizeCurrency(String currency) {
        if (currency == null || currency.isBlank()) {
            return DEFAULT_CURRENCY;
        }
        String code = currency.trim().toUpperCase(Locale.ROOT);
        try {
            Currency.getInstance(code);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Moeda inválida: '" + currency + "'. Use um código ISO 4217 (ex.: BRL).");
        }
        return code;
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
