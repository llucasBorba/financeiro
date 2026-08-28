package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindGoalUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.FinancialGoalRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FinancialGoalService implements CreateGoalUseCase, DepositToGoalUseCase, FindGoalUseCase {

    private final FinancialGoalRepositoryPort goalRepository;

    public FinancialGoalService(FinancialGoalRepositoryPort goalRepository) {
        this.goalRepository = Objects.requireNonNull(goalRepository, "O repositório de metas é obrigatório.");
    }

    @Override
    public FinancialGoal execute(CreateGoalCommand command) {
        Money targetAmount = new Money(command.targetAmount(), command.currency());

        FinancialGoal goal = new FinancialGoal(
                null,
                command.userId(),
                command.title(),
                targetAmount,
                command.targetDate()
        );

        return goalRepository.save(goal);
    }

    @Override
    public FinancialGoal execute(DepositCommand command) {
        FinancialGoal goal = findById(command.goalId(), command.userId());

        Money depositAmount = new Money(command.amount(), command.currency());

        // Atualiza o valor atual na própria entidade
        goal.deposit(depositAmount);

        return goalRepository.save(goal);
    }

    @Override
    public FinancialGoal findById(UUID goalId, UUID userId) {
        FinancialGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta financeira não encontrada com o ID: " + goalId));

        if (!goal.isOwnedBy(userId)) {
            throw new BusinessException("Acesso negado: a meta não pertence ao usuário informado.");
        }

        return goal;
    }

    @Override
    public List<FinancialGoal> listByUser(UUID userId) {
        return goalRepository.findByUserId(userId);
    }
}
