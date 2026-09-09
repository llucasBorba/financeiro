package br.com.gastos.financeiro.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Clock;

/**
 * Regras de acesso da API.
 *
 * <p>Substitui a configuração anterior, que liberava tudo com {@code anyRequest().permitAll()}.
 * A partir daqui, <strong>tudo sob {@code /api/**} exige um token válido</strong>.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String[] PUBLIC_PATHS = {
            "/auth/register",
            "/auth/login",
            "/auth/google",
            "/v3/api-docs/**",
            "/swagger-ui/**",
            "/swagger-ui.html"
    };

    /**
     * Relógio da aplicação, como bean para poder ser substituído nos testes.
     *
     * <p>Existe por causa do {@link LoginAttemptLimiter}: sem injetar o relógio, testar que a
     * trava expira em 15 minutos exigiria esperar 15 minutos.
     */
    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtDecoder jwtDecoder,
            @Qualifier("handlerExceptionResolver") HandlerExceptionResolver exceptionResolver) throws Exception {

        AuthenticationEntryPoint entryPoint = problemDetailEntryPoint(exceptionResolver);

        http
                // CSRF protege sessões baseadas em cookie. Esta API é stateless e autentica por
                // header Authorization, que o navegador não envia sozinho em requisição de outro
                // site — o ataque que o CSRF previne não se aplica aqui.
                .csrf(csrf -> csrf.disable())

                // Sem sessão no servidor: cada requisição se prova sozinha pelo token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())

                // Valida o Bearer token e popula o SecurityContext que o JwtCurrentUserProvider lê.
                //
                // O entry point precisa ser declarado AQUI TAMBÉM: o resource server instala o
                // BearerTokenAuthenticationEntryPoint próprio, que tem precedência sobre o global
                // do exceptionHandling. Sem isto, token inválido responderia 401 com corpo vazio
                // enquanto token ausente responderia ProblemDetail — dois contratos diferentes
                // para o mesmo status.
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.decoder(jwtDecoder))
                        .authenticationEntryPoint(entryPoint))

                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler((request, response, ex) ->
                                exceptionResolver.resolveException(request, response, null, ex)));

        return http.build();
    }

    /**
     * Responde 401 no mesmo formato ProblemDetail do resto da API.
     *
     * <p>Também substitui o {@code WWW-Authenticate} detalhado que o Spring Security produz por
     * padrão. O original diz coisas como
     * {@code error_description="Signed JWT rejected: Invalid signature"} — o que informa ao
     * atacante <em>qual</em> checagem falhou (assinatura? validade? formato?). É o mesmo tipo de
     * pista que evitamos no login ao usar uma única mensagem para toda falha de credencial.
     * O header continua presente, como pede a RFC 6750, mas sem o diagnóstico.
     */
    private static AuthenticationEntryPoint problemDetailEntryPoint(HandlerExceptionResolver resolver) {
        return (request, response, ex) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            resolver.resolveException(request, response, null, ex);
        };
    }
}
