package br.com.gastos.financeiro.infrastructure.identity;

import org.springframework.web.context.request.NativeWebRequest;

import java.util.UUID;

/**
 * Descobre quem é o usuário por trás da requisição atual.
 *
 * <p>Este é o único ponto do sistema que sabe de onde vem a identidade. Hoje existe uma
 * implementação de desenvolvimento ({@link DevHeaderCurrentUserProvider}); na fase de
 * autenticação entra uma que lê o {@code sub} do JWT validado pelo Spring Security.
 *
 * <p>Não há implementação registrada fora do profile {@code dev}: em produção, a ausência
 * do bean impede a aplicação de subir. Isso é intencional — é preferível um deploy que
 * falha em 30 segundos a um que sobe com a identidade desprotegida.
 */
public interface CurrentUserProvider {

    /**
     * @return o id do usuário da requisição atual
     * @throws IllegalArgumentException se a identidade vier em formato inválido
     */
    UUID requireCurrentUser(NativeWebRequest request);
}
