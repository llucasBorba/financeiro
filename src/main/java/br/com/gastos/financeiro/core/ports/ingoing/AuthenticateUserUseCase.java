package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.User;

public interface AuthenticateUserUseCase {

    record LoginCommand(String email, String password) {}

    User execute(LoginCommand command);
}
