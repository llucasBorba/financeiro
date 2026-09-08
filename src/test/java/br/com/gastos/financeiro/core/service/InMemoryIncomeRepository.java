package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.ports.outgoing.IncomeRepositoryPort;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adapter de teste para {@link IncomeRepositoryPort}: um HashMap no lugar do Postgres. */
public class InMemoryIncomeRepository implements IncomeRepositoryPort {

    private final Map<UUID, Income> storage = new LinkedHashMap<>();

    @Override
    public Income save(Income income) {
        storage.put(income.getId(), income);
        return income;
    }

    @Override
    public Optional<Income> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public List<Income> findByUserId(UUID userId) {
        return storage.values().stream()
                .filter(i -> i.getUserId().equals(userId))
                .sorted(Comparator.comparing(Income::getReceivedAt).reversed())
                .toList();
    }

    @Override
    public List<Income> findByUserIdAndReceivedAtBetween(UUID userId, LocalDate startDate, LocalDate endDate) {
        return findByUserId(userId).stream()
                .filter(i -> !i.getReceivedAt().isBefore(startDate) && !i.getReceivedAt().isAfter(endDate))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}
