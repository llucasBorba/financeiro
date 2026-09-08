package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Category;

import java.util.UUID;

public interface RenameCategoryUseCase {

    record RenameCategoryCommand(UUID categoryId, UUID userId, String name) {}

    Category execute(RenameCategoryCommand command);
}
