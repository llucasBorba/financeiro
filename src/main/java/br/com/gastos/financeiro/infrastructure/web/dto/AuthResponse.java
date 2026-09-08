package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.infrastructure.config.security.JwtIssuer.IssuedToken;

/**
 * Resposta de qualquer caminho de login.
 *
 * <p>Segue o vocabulário do OAuth 2.0 ({@code access_token}, {@code token_type},
 * {@code expires_in}) porque é o que todo cliente HTTP já sabe consumir.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user
) {
    private static final String BEARER = "Bearer";

    public static AuthResponse of(IssuedToken token, User user) {
        return new AuthResponse(token.value(), BEARER, token.expiresInSeconds(), UserResponse.from(user));
    }
}
