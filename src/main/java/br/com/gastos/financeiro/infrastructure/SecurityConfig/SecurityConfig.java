package br.com.gastos.financeiro.infrastructure.SecurityConfig;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Libera todas as suas rotas de API REST de forma limpa
                        .anyRequest().permitAll()
                );

        return http.build();
    }

    /**
     * BOA PRÁTICA: WebSecurityCustomizer instrui o Spring Security a ignorar
     * completamente estes caminhos de recursos estáticos, evitando que qualquer
     * filtro intercepte e quebre o carregamento do Swagger UI.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers(
                "/v3/api-docs/**",
                "/swagger-ui/**",
                "/swagger-ui.html",
                "/webjars/**"
        );
    }
}