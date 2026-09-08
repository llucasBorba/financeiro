package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.RecurringExpense;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/** Cria o modelo e já gera as primeiras ocorrências. */
public interface CreateRecurringExpenseUseCase {

    record CreateRecurringExpenseCommand(
            UUID userId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            String description,
            int dayOfMonth,
            YearMonth startMonth,
            YearMonth endMonth
    ) {}

    RecurringExpense execute(CreateRecurringExpenseCommand command);
}
