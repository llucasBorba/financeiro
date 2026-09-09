package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.Budget;
import br.com.gastos.financeiro.core.ports.outgoing.BudgetRepositoryPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adapter de teste: substitui o banco sem precisar subir o Spring. */
class InMemoryBudgetRepository implements BudgetRepositoryPort {

    private final Map<UUID, Budget> storage = new LinkedHashMap<>();

    @Override
    public Budget save(Budget budget) {
        storage.put(budget.getId(), budget);
        return budget;
    }

    @Override
    public List<Budget> findByUserId(UUID userId) {
        List<Budget> result = new ArrayList<>();
        for (Budget budget : storage.values()) {
            if (budget.isOwnedBy(userId)) {
                result.add(budget);
            }
        }
        return result;
    }

    @Override
    public Optional<Budget> findByUserIdAndCategoryId(UUID userId, UUID categoryId) {
        return storage.values().stream()
                .filter(b -> b.isOwnedBy(userId) && b.getCategoryId().equals(categoryId))
                .findFirst();
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}
