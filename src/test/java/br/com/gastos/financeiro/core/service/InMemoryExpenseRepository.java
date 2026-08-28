package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adapter de teste: substitui o banco sem precisar subir o Spring. */
class InMemoryExpenseRepository implements ExpenseRepositoryPort {

    private final Map<UUID, Expense> storage = new LinkedHashMap<>();

    @Override
    public Expense save(Expense expense) {
        storage.put(expense.getId(), expense);
        return expense;
    }

    @Override
    public Optional<Expense> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Expense> findByUserId(UUID userId) {
        List<Expense> result = new ArrayList<>();
        for (Expense expense : storage.values()) {
            if (expense.isOwnedBy(userId)) {
                result.add(expense);
            }
        }
        return result;
    }

    @Override
    public List<Expense> findByUserIdAndDueDateBetween(UUID userId, LocalDate startDate, LocalDate endDate) {
        List<Expense> result = new ArrayList<>();
        for (Expense expense : findByUserId(userId)) {
            LocalDate dueDate = expense.getDueDate();
            if (!dueDate.isBefore(startDate) && !dueDate.isAfter(endDate)) {
                result.add(expense);
            }
        }
        return result;
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}
