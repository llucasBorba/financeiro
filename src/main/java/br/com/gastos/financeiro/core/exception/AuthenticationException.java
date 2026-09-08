package br.com.gastos.financeiro.core.exception;

/**
 * Credencial ausente, inválida ou expirada — vira 401 na borda HTTP.
 *
 * <p>Separada de {@link BusinessException} de propósito: "você não provou quem é" (401) é
 * diferente de "a regra de negócio não permite" (400) e de "sei quem você é, mas não pode" (403).
 */
public class AuthenticationException extends RuntimeException {
    public AuthenticationException(String message) {
        super(message);
    }
}
