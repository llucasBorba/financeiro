package br.com.gastos.financeiro.infrastructure.config.security;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.time.Clock;
import java.time.Duration;
import java.util.List;

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

    /**
     * Libera o front a chamar esta API de outra origem.
     *
     * <p>O navegador bloqueia por padrão requisições de um site para outra origem — e
     * {@code localhost:3000} e {@code localhost:8080} são origens diferentes, porque a porta
     * entra na conta. Sem esta configuração a chamada nem chega ao servidor: o navegador a
     * barra antes, então não há log nenhum deste lado para consultar, e o erro no console não
     * diz "faltou CORS", parece falha de rede.
     *
     * <p>A origem vem do ambiente e não fica escrita no código: versionar {@code localhost}
     * significaria que produção sobe apontando para a máquina de alguém.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") List<String> allowedOrigins) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // Authorization precisa estar aqui, senão o preflight é aprovado e a requisição real é
        // barrada por causa do header do token — sintoma confuso: funciona em rota pública e
        // falha em rota autenticada.
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));

        // O JavaScript só enxerga um punhado de headers de resposta por padrão; estes dois
        // ficariam invisíveis para o front sem serem declarados aqui.
        // Location: devolvido pelos POST de criação, com a URL do recurso novo.
        // Retry-After: quanto falta para sair da trava de tentativas de login — sem ele, a tela
        // de erro não consegue dizer ao usuário quando tentar de novo.
        config.setExposedHeaders(List.of("Location", "Retry-After"));

        // Sem allowCredentials: a autenticação é por header Authorization, que o front envia
        // explicitamente. Só vira necessário se um dia o refresh token for guardado em cookie
        // HttpOnly — e aí CSRF, hoje desligado com razão, volta a precisar de atenção.

        // Cacheia o preflight por 1h: sem isto o navegador manda um OPTIONS antes de cada
        // requisição, dobrando o número de idas ao servidor.
        config.setMaxAge(Duration.ofHours(1));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
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

                // Precisa ser chamado explicitamente: sem isto o bean corsConfigurationSource
                // abaixo existiria e nunca seria consultado, e o navegador barraria toda
                // requisição do front sem que nada aparecesse no log do servidor.
                .cors(Customizer.withDefaults())

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
