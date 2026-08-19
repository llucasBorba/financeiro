package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.FinancialGoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateGoalUseCase {

    record CreateGoalCommand(
            UUID userId,
            String title,
            BigDecimal targetAmount,
            String currency,
            LocalDate targetDate
    ) {}

    FinancialGoal execute(CreateGoalCommand command);
}
