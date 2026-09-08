package br.com.gastos.financeiro.infrastructure.config.security;

import br.com.gastos.financeiro.core.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;

/**
 * Emite o token de acesso da aplicação.
 *
 * <p>O {@code sub} carrega o <strong>UUID interno</strong> do usuário, não o id do Google.
 * A RFC 7519 pede que o {@code sub} seja único no contexto do emissor — e o emissor aqui somos
 * nós. Isso também evita uma consulta ao banco por requisição: o {@code JwtCurrentUserProvider}
 * lê o id direto do token, sem precisar traduzir nada.
 *
 * <p>Nada sensível entra nos claims: o payload de um JWT é apenas assinado, <strong>não é
 * criptografado</strong>. Qualquer um com o token lê o conteúdo.
 */
@Component
public class JwtIssuer {

    public record IssuedToken(String value, long expiresInSeconds) {}

    private final JwtEncoder encoder;
    private final String issuer;
    private final Duration expiration;

    /*
     * Emissor e validade têm padrão no código porque não são segredos — se sumirem da
     * configuração, o sistema segue coerente. O segredo de assinatura NÃO tem padrão
     * (ver JwtConfig): faltando ele, é melhor a aplicação não subir.
     */
    public JwtIssuer(JwtEncoder encoder,
                     @Value("${app.jwt.issuer:financeiro-api}") String issuer,
                     @Value("${app.jwt.expiration:PT1H}") Duration expiration) {
        this.encoder = encoder;
        this.issuer = issuer;
        this.expiration = expiration;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject(user.getId().toString())
                .issuedAt(now)
                .expiresAt(now.plus(expiration))
                .claim("email", user.getEmail().value())
                .claim("name", user.getName())
                .build();

        String token = encoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)
        ).getTokenValue();

        return new IssuedToken(token, expiration.toSeconds());
    }
}
