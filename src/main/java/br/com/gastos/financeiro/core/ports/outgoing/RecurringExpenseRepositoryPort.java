package br.com.gastos.financeiro.core.ports.outgoing;

import br.com.gastos.financeiro.core.model.RecurringExpense;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecurringExpenseRepositoryPort {

    RecurringExpense save(RecurringExpense recurringExpense);

    Optional<RecurringExpense> findById(UUID id);

    List<RecurringExpense> findByUserId(UUID userId);

    void deleteById(UUID id);
}
