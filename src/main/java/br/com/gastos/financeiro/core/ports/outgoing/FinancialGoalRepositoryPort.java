package br.com.gastos.financeiro.core.ports.outgoing;


import br.com.gastos.financeiro.core.model.FinancialGoal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
public interface FinancialGoalRepositoryPort {

    FinancialGoal save(FinancialGoal goal);

    Optional<FinancialGoal> findById(UUID id);

    List<FinancialGoal> findByUserId(UUID userId);

    void deleteById(UUID id);
}
