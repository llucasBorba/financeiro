package br.com.gastos.financeiro.core.ports.outgoing;

import br.com.gastos.financeiro.core.model.Budget;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BudgetRepositoryPort {

    Budget save(Budget budget);

    List<Budget> findByUserId(UUID userId);

    /**
     * O orçamento de uma categoria, se houver.
     *
     * <p>Busca por (dono, categoria) e não por id porque é assim que o recurso é endereçado:
     * existe no máximo um orçamento por categoria, então a categoria é a identidade natural.
     * É também o que torna a gravação idempotente — a mesma requisição repetida encontra o
     * que já existe e atualiza, em vez de criar um segundo.
     */
    Optional<Budget> findByUserIdAndCategoryId(UUID userId, UUID categoryId);

    void deleteById(UUID id);
}
