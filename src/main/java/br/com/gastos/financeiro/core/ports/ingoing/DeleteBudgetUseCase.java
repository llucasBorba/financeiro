package br.com.gastos.financeiro.core.ports.ingoing;

import java.util.UUID;

/** Remove o limite de uma categoria. A categoria e suas despesas continuam existindo. */
public interface DeleteBudgetUseCase {

    void execute(UUID userId, UUID categoryId);
}
