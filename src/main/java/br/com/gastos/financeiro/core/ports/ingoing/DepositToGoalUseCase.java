package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.FinancialGoal;

import java.math.BigDecimal;
import java.util.UUID;

public interface DepositToGoalUseCase {

    record DepositCommand(
            UUID goalId,
            UUID userId,
            BigDecimal amount,
            String currency
    ) {}

    FinancialGoal execute(DepositCommand command);
}
