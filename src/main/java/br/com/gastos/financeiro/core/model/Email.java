package br.com.gastos.financeiro.core.model;

import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Value Object de endereço de e-mail.
 *
 * <p>Existe pelo mesmo motivo de {@link Money}: normalizar num lugar só. E-mail é a chave de
 * login e precisa ser único — se {@code "Lucas@Gmail.com"} e {@code "lucas@gmail.com"} virassem
 * duas linhas diferentes, a unicidade seria uma ficção e a mesma pessoa teria duas contas.
 *
 * <p>A validação de formato é deliberadamente simples. Validar e-mail pela RFC 5322 completa é
 * um exercício inútil: o único teste real de que um endereço existe é mandar uma mensagem para ele.
 */
public class Email {

    private static final Pattern FORMAT = Pattern.compile("^[^@\\s]+@[^@\\s.]+(\\.[^@\\s.]+)+$");
    private static final int MAX_LENGTH = 254;

    private final String value;

    public Email(String value) {
        Objects.requireNonNull(value, "O e-mail é obrigatório.");
        String normalized = value.trim().toLowerCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("O e-mail é obrigatório.");
        }
        if (normalized.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("O e-mail deve ter no máximo " + MAX_LENGTH + " caracteres.");
        }
        if (!FORMAT.matcher(normalized).matches()) {
            throw new IllegalArgumentException("E-mail inválido: '" + value + "'.");
        }
        this.value = normalized;
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        return value.equals(((Email) o).value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public String toString() {
        return value;
    }
}
