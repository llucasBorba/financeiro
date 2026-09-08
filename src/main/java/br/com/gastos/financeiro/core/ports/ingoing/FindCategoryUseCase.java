package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Category;

import java.util.List;
import java.util.UUID;

public interface FindCategoryUseCase {

    Category findById(UUID categoryId, UUID userId);

    /** Por padrão traz só as ativas — arquivadas não devem poluir um seletor. */
    List<Category> listByUser(UUID userId, boolean includeInactive);
}
