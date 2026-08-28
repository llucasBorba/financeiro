package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.FinancialGoal;

import java.util.List;
import java.util.UUID;

public interface FindGoalUseCase {

    FinancialGoal findById(UUID goalId, UUID userId);

    List<FinancialGoal> listByUser(UUID userId);
}
