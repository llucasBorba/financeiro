package br.com.gastos.financeiro.core.ports.ingoing;

import java.util.UUID;

public interface DeleteCategoryUseCase {

    /** Só remove categoria sem nenhuma despesa vinculada. Em uso, arquive em vez de apagar. */
    void execute(UUID categoryId, UUID userId);
}
