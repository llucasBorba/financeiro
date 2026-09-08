package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Category;

import java.util.UUID;

public interface CreateCategoryUseCase {

    record CreateCategoryCommand(UUID userId, String name) {}

    Category execute(CreateCategoryCommand command);
}
