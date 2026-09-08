package br.com.gastos.financeiro.infrastructure.identity;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registra o {@link CurrentUserArgumentResolver} no Spring MVC.
 *
 * <p>Recebe o {@link CurrentUserProvider} por construtor: se nenhum bean desse tipo existir
 * — o caso de qualquer profile que não seja {@code dev}, enquanto a autenticação real não
 * chega — a aplicação falha ao subir, em vez de atender requisições sem saber quem é quem.
 */
@Configuration
public class CurrentUserWebConfig implements WebMvcConfigurer {

    private final CurrentUserProvider currentUserProvider;

    public CurrentUserWebConfig(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new CurrentUserArgumentResolver(currentUserProvider));
    }
}
