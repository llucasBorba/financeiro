package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.ports.outgoing.FinancialGoalRepositoryPort;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adapter de teste: substitui o banco sem precisar subir o Spring. */
class InMemoryFinancialGoalRepository implements FinancialGoalRepositoryPort {

    private final Map<UUID, FinancialGoal> storage = new LinkedHashMap<>();

    @Override
    public FinancialGoal save(FinancialGoal goal) {
        storage.put(goal.getId(), goal);
        return goal;
    }

    @Override
    public Optional<FinancialGoal> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<FinancialGoal> findByUserId(UUID userId) {
        List<FinancialGoal> result = new ArrayList<>();
        for (FinancialGoal goal : storage.values()) {
            if (goal.isOwnedBy(userId)) {
                result.add(goal);
            }
        }
        return result;
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}
