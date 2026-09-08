package br.com.gastos.financeiro.infrastructure.config.security;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * Chaves de assinatura dos tokens que <strong>nós</strong> emitimos.
 *
 * <p>Usamos HS256 (HMAC com segredo compartilhado): a mesma chave assina e confere. É a escolha
 * certa quando quem emite e quem valida são o mesmo serviço — o nosso caso. Se um dia outros
 * serviços precisarem validar nossos tokens sem poder emiti-los, a troca é para RS256 (par de
 * chaves), e só este arquivo muda.
 *
 * <p><strong>O segredo nunca fica versionado.</strong> Fora do profile {@code dev} ele é
 * obrigatório e a aplicação recusa subir sem ele; em desenvolvimento, quando ausente, um segredo
 * aleatório é gerado a cada inicialização.
 *
 * <p>Antes havia um valor literal em {@code application-dev.properties}. Como
 * {@code spring.profiles.active} tem {@code dev} por padrão, um deploy que esquecesse de definir
 * o profile passaria a assinar tokens com um valor público — e qualquer pessoa que lesse o
 * repositório poderia forjar um token para qualquer usuário. Gerar aleatoriamente elimina a
 * classe inteira do problema: não existe valor conhecido para vazar.
 */
@Configuration
public class JwtConfig {

    private static final Logger log = LoggerFactory.getLogger(JwtConfig.class);

    /** HS256 exige chave de no mínimo 256 bits. Chave curta = assinatura fraca. */
    private static final int MIN_SECRET_BYTES = 32;

    private final SecretKey secretKey;

    public JwtConfig(@Value("${app.jwt.secret:}") String secret, Environment environment) {
        this.secretKey = new SecretKeySpec(resolveSecret(secret, environment), "HmacSHA256");
    }

    private static byte[] resolveSecret(String configurado, Environment environment) {
        if (!configurado.isBlank()) {
            byte[] bytes = configurado.getBytes(StandardCharsets.UTF_8);
            if (bytes.length < MIN_SECRET_BYTES) {
                throw new IllegalStateException(
                        "app.jwt.secret precisa ter ao menos " + MIN_SECRET_BYTES
                                + " bytes para HS256 (tem " + bytes.length + ").");
            }
            return bytes;
        }

        // Fora de desenvolvimento, segredo ausente é erro de implantação — não algo a contornar.
        if (!environment.matchesProfiles("dev")) {
            throw new IllegalStateException(
                    "Defina a variável de ambiente JWT_SECRET com pelo menos " + MIN_SECRET_BYTES
                            + " bytes. Ela não tem valor padrão de propósito: segredo com padrão "
                            + "é segredo público.");
        }

        byte[] aleatorio = new byte[MIN_SECRET_BYTES];
        new SecureRandom().nextBytes(aleatorio);
        log.warn("Nenhum JWT_SECRET definido: gerado um segredo aleatório para esta execução. "
                + "Os tokens emitidos deixam de valer a cada reinício. "
                + "Defina JWT_SECRET (>= {} bytes) se quiser que sobrevivam.", MIN_SECRET_BYTES);
        return aleatorio;
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
