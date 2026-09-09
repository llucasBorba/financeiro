package br.com.gastos.financeiro.infrastructure.config.security;

import java.time.Duration;

/**
 * Tentativas de login demais para a mesma combinação de e-mail e origem.
 *
 * <p>Vive na infraestrutura, e não no {@code core}, porque limite de requisição é preocupação
 * de transporte HTTP: o domínio não sabe o que é um IP nem o que é uma requisição.
 */
public class TooManyLoginAttemptsException extends RuntimeException {

    private final Duration retryAfter;

    public TooManyLoginAttemptsException(Duration retryAfter) {
        super("Tentativas de login demais. Tente novamente mais tarde.");
        this.retryAfter = retryAfter;
    }

    public Duration getRetryAfter() {
        return retryAfter;
    }
}
