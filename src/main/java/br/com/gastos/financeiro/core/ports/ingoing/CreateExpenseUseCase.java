package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateExpenseUseCase {

    record CreateExpenseCommand(
            UUID userId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate dueDate,
            String type
    ) {}

    Expense execute(CreateExpenseCommand command);
}
