package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.RecurringExpense;

import java.util.List;
import java.util.UUID;

public interface FindRecurringExpenseUseCase {

    RecurringExpense findById(UUID recurringExpenseId, UUID userId);

    List<RecurringExpense> listByUser(UUID userId);
}
