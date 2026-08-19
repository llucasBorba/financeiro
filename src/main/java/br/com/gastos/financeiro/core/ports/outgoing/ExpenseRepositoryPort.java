package br.com.gastos.financeiro.core.ports.outgoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepositoryPort {

    Expense save(Expense expense);

    Optional<Expense> findById(UUID id);

    List<Expense> findByUserId(UUID userId);

    List<Expense> findByUserIdAndDueDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    void deleteById(UUID id);
}

