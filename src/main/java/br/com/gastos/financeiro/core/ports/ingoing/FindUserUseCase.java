package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.User;

import java.util.UUID;

public interface FindUserUseCase {

    User findById(UUID userId);
}
