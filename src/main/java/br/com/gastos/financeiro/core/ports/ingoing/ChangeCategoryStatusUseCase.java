package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Category;

import java.util.UUID;

/**
 * Arquiva ou reativa uma categoria.
 *
 * <p>Separado de {@link RenameCategoryUseCase} porque são operações com semântica diferente:
 * renomear substitui um dado, arquivar muda o ciclo de vida. Na borda HTTP isso vira
 * {@code PUT} e {@code PATCH}, respectivamente.
 */
public interface ChangeCategoryStatusUseCase {

    record ChangeCategoryStatusCommand(UUID categoryId, UUID userId, boolean active) {}

    Category execute(ChangeCategoryStatusCommand command);
}
