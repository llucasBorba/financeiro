package br.com.gastos.financeiro.infrastructure.config.security;

import br.com.gastos.financeiro.core.exception.AuthenticationException;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUserProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.NativeWebRequest;

import java.util.UUID;

/**
 * Descobre o usuário a partir do JWT já validado pelo Spring Security.
 *
 * <p>Quando este código roda, o filtro do resource server já conferiu assinatura, emissor e
 * expiração — um token adulterado ou vencido nunca chega até aqui. Só resta ler o {@code sub}.
 *
 * <p>É a única implementação de {@link CurrentUserProvider} do sistema. O provider de
 * desenvolvimento que lia o header {@code X-User-Id} foi removido junto com a entrada da
 * autenticação real: manter os dois lado a lado seria manter uma porta dos fundos.
 */
@Component
public class JwtCurrentUserProvider implements CurrentUserProvider {

    @Override
    public UUID requireCurrentUser(NativeWebRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new AuthenticationException("Autenticação obrigatória para acessar este recurso.");
        }

        try {
            return UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException e) {
            // Só acontece com token emitido por outra versão do sistema — vale tratar como inválido.
            throw new AuthenticationException("Token inválido: o identificador do usuário não é utilizável.");
        }
    }
}
