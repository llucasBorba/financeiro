package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.User;

public interface RegisterUserUseCase {

    record RegisterCommand(String email, String password, String name) {}

    User execute(RegisterCommand command);
}
