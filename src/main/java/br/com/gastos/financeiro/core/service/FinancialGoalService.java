package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.FinancialGoalRepositoryPort;

import java.util.Objects;

public class FinancialGoalService implements CreateGoalUseCase, DepositToGoalUseCase {

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
        FinancialGoal goal = goalRepository.findById(command.goalId())
                .orElseThrow(() -> new ResourceNotFoundException("Meta financeira não encontrada com o ID: " + command.goalId()));

        if (!goal.getUserId().equals(command.userId())) {
            throw new BusinessException("Acesso negado: a meta não pertence ao usuário informado.");
        }

        Money depositAmount = new Money(command.amount(), command.currency());

        // Atualiza o valor atual na própria entidade
        goal.deposit(depositAmount);

        return goalRepository.save(goal);
    }
}