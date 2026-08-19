package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.time.LocalDateTime;
import java.util.UUID;
public interface MarkExpenseAsPaidUseCase {
    record MarkAsPaidCommand(
            UUID expenseId,
            UUID userId,
            LocalDateTime paymentDate
    ) {}

    Expense execute(MarkAsPaidCommand command);
}
