package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.User;

/**
 * Recebe os dados <strong>já validados</strong> de um token do Google.
 *
 * <p>Quem confere assinatura, emissor e audiência é o adapter na infraestrutura. O domínio
 * assume que essa checagem aconteceu e cuida apenas da regra: achar a conta, vincular ou criar.
 */
public interface AuthenticateWithGoogleUseCase {

    record GoogleLoginCommand(String googleId, String email, String name, boolean emailVerified) {}

    User execute(GoogleLoginCommand command);
}
