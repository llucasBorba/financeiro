package br.com.gastos.financeiro.infrastructure.config.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

/**
 * Chaves de assinatura dos tokens que <strong>nós</strong> emitimos.
 *
 * <p>Usamos HS256 (HMAC com segredo compartilhado): a mesma chave assina e confere. É a escolha
 * certa quando quem emite e quem valida são o mesmo serviço — o nosso caso. Se um dia outros
 * serviços precisarem validar nossos tokens sem poder emiti-los, a troca é para RS256 (par de
 * chaves), e só este arquivo muda.
 *
 * <p>O segredo vem de configuração e <strong>não tem valor padrão em produção</strong>: sem a
 * variável {@code JWT_SECRET}, a aplicação não sobe. Segredo com padrão é segredo público.
 */
@Configuration
public class JwtConfig {

    /** HS256 exige chave de no mínimo 256 bits. Chave curta = assinatura fraca. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public JwtConfig(@Value("${app.jwt.secret}") String secret) {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret precisa ter ao menos " + MIN_SECRET_BYTES
                            + " bytes para HS256 (tem " + bytes.length + ").");
        }
        this.secretKey = new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey));
    }

    /**
     * Decoder dos <em>nossos</em> tokens. Confere assinatura e expiração automaticamente;
     * um token adulterado ou vencido nunca chega ao controller.
     */
    @Bean
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
