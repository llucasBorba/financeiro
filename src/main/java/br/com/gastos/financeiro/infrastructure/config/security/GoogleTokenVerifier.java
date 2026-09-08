package br.com.gastos.financeiro.infrastructure.config.security;

import br.com.gastos.financeiro.core.exception.AuthenticationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * Confere o {@code id_token} emitido pelo Google e extrai os dados da conta.
 *
 * <p>Substitui o {@code GoogleAuthService} anterior, que criava um {@code NimbusJwtDecoder}
 * novo a cada requisição — jogando fora o cache das chaves públicas do Google e batendo no
 * endpoint dele toda vez. Aqui o decoder é montado uma única vez, no construtor de um bean
 * singleton, e cuida do cache sozinho.
 *
 * <p>Três checagens precisam passar, e todas importam:
 * <ul>
 *   <li><strong>assinatura</strong> — o token foi mesmo emitido pelo Google (chaves do JWKS);</li>
 *   <li><strong>emissor</strong> — {@code iss} é o Google, e não outro provedor qualquer;</li>
 *   <li><strong>audiência</strong> — {@code aud} é o <em>nosso</em> client id. Sem isso, um token
 *       válido emitido para <em>outro</em> aplicativo seria aceito aqui, e o dono daquele
 *       aplicativo entraria na conta de qualquer usuário nosso.</li>
 * </ul>
 */
@Component
public class GoogleTokenVerifier {

    /** Chaves públicas do Google. O decoder busca e mantém em cache automaticamente. */
    private static final String JWK_SET_URI = "https://www.googleapis.com/oauth2/v3/certs";

    /** O Google emite tokens com as duas formas; ambas são legítimas. */
    private static final Set<String> VALID_ISSUERS =
            Set.of("https://accounts.google.com", "accounts.google.com");

    /** Dados de conta extraídos de um token já verificado. */
    public record GoogleAccount(String googleId, String email, String name, boolean emailVerified) {}

    private final NimbusJwtDecoder decoder;

    public GoogleTokenVerifier(@Value("${google.oauth.client-id}") String clientId) {
        NimbusJwtDecoder googleDecoder = NimbusJwtDecoder.withJwkSetUri(JWK_SET_URI).build();
        googleDecoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(),      // expiração e "not before"
                issuerValidator(),
                audienceValidator(clientId)));
        this.decoder = googleDecoder;
    }

    public GoogleAccount verify(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new AuthenticationException("O id_token do Google é obrigatório.");
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (RuntimeException e) {
            // A mensagem original pode expor detalhes internos; o cliente só precisa saber que falhou.
            throw new AuthenticationException("Token do Google inválido ou expirado.");
        }

        String email = jwt.getClaimAsString("email");
        if (email == null) {
            throw new AuthenticationException("O token do Google não trouxe um e-mail.");
        }

        return new GoogleAccount(
                jwt.getSubject(),
                email,
                jwt.getClaimAsString("name"),
                Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified")));
    }

    private static OAuth2TokenValidator<Jwt> issuerValidator() {
        return jwt -> {
            String issuer = jwt.getIssuer() != null ? jwt.getIssuer().toString() : null;
            return VALID_ISSUERS.contains(issuer)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_issuer", "Emissor inesperado: " + issuer, null));
        };
    }

    private static OAuth2TokenValidator<Jwt> audienceValidator(String clientId) {
        return jwt -> {
            List<String> audience = jwt.getAudience();
            return audience != null && audience.contains(clientId)
                    ? OAuth2TokenValidatorResult.success()
                    : OAuth2TokenValidatorResult.failure(
                            new OAuth2Error("invalid_audience",
                                    "O token não foi emitido para esta aplicação.", null));
        };
    }
}
