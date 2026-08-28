package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FindExpenseUseCase {

    Expense findById(UUID expenseId, UUID userId);

    List<Expense> listByUser(UUID userId);

    List<Expense> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate);
}
