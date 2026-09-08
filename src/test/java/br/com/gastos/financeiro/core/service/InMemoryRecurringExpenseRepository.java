package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.RecurringExpense;
import br.com.gastos.financeiro.core.ports.outgoing.RecurringExpenseRepositoryPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryRecurringExpenseRepository implements RecurringExpenseRepositoryPort {

    private final Map<UUID, RecurringExpense> storage = new LinkedHashMap<>();

    @Override
    public RecurringExpense save(RecurringExpense recurringExpense) {
        storage.put(recurringExpense.getId(), recurringExpense);
        return recurringExpense;
    }

    @Override
    public Optional<RecurringExpense> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<RecurringExpense> findByUserId(UUID userId) {
        return storage.values().stream().filter(r -> r.getUserId().equals(userId)).toList();
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}
