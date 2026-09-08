package br.com.gastos.financeiro.infrastructure.identity;

import org.springframework.core.MethodParameter;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.UUID;

/**
 * Ensina o Spring MVC a preencher parâmetros anotados com {@link CurrentUser}.
 *
 * <p>É o mesmo mecanismo por trás de {@code @PathVariable} e {@code @RequestParam}: antes
 * de invocar o método do controller, o Spring pergunta a cada resolver registrado
 * "você sabe montar este parâmetro?" e usa o primeiro que responder que sim.
 *
 * <p>Toda a decisão de <em>onde</em> a identidade mora fica no {@link CurrentUserProvider};
 * este resolver só faz a ponte com o Spring MVC.
 */
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final CurrentUserProvider currentUserProvider;

    public CurrentUserArgumentResolver(CurrentUserProvider currentUserProvider) {
        this.currentUserProvider = currentUserProvider;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && UUID.class.equals(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter,
                                  ModelAndViewContainer mavContainer,
                                  NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        return currentUserProvider.requireCurrentUser(webRequest);
    }
}
