package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.FinancialGoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Substituição dos dados editáveis da meta. O saldo acumulado não está aqui: ele muda por
 * aporte, nunca por edição.
 */
public interface UpdateGoalUseCase {

    record UpdateGoalCommand(
            UUID goalId,
            UUID userId,
            String title,
            BigDecimal targetAmount,
            String currency,
            LocalDate targetDate
    ) {}

    FinancialGoal execute(UpdateGoalCommand command);
}
