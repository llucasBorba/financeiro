package br.com.gastos.financeiro.infrastructure.googleAuth.Service;


import br.com.gastos.financeiro.infrastructure.googleAuth.Dto.GoogleUserInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Service;

@Service
public class GoogleAuthService {

    @Value("${google.oauth.client-id}")
    private String webClientId;

    private final String googleJwkSetUri = "https://www.googleapis.com/oauth2/v3/certs";

    public GoogleUserInfo dadosUser(String token) {
        try {

            JwtDecoder jwtDecoder = NimbusJwtDecoder.withJwkSetUri(googleJwkSetUri).build();

            Jwt jwt = jwtDecoder.decode(token);

            // Validação de boa prática: Garantir que o token foi gerado para o SEU Client ID (Audience)
            if (!jwt.getAudience().contains(webClientId)) {
                throw new RuntimeException("Token não pertence a esta aplicação (Audience inválido).");
            }

            // Extrai as informações (Claims) do payload do JWT
            String userId = jwt.getSubject(); // O ID único do usuário no Google
            String email = jwt.getClaimAsString("email");
            boolean emailVerified = Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"));
            String name = jwt.getClaimAsString("name");
            String pictureUrl = jwt.getClaimAsString("picture");

            return new GoogleUserInfo(userId, email, name, pictureUrl, emailVerified);

        } catch (Exception e) {
            // Boa prática: Em produção, use um Logger (ex: Slf4j). Evite printStackTrace ou Runtime puro se puder tratar.
            throw new RuntimeException("Falha na autenticação Google: " + e.getMessage(), e);
        }
    }
}
