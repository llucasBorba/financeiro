package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.RecurringExpense;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Altera o modelo e propaga para as ocorrências <strong>futuras e não pagas</strong>.
 *
 * <p>Ocorrências já pagas ficam intocadas — o que você pagou é fato consumado. E as vencidas
 * não pagas também: se o aluguel de agosto era R$ 1.500, você deve R$ 1.500, mesmo que o valor
 * tenha subido depois.
 */
public interface UpdateRecurringExpenseUseCase {

    record UpdateRecurringExpenseCommand(
            UUID recurringExpenseId,
            UUID userId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            String description,
            int dayOfMonth,
            YearMonth endMonth
    ) {}

    RecurringExpense execute(UpdateRecurringExpenseCommand command);
}
