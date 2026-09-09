package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.WithdrawFromGoalUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.FinancialGoalRepositoryPort;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class FinancialGoalService implements CreateGoalUseCase, UpdateGoalUseCase,
        DeleteGoalUseCase, DepositToGoalUseCase, WithdrawFromGoalUseCase, FindGoalUseCase {

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
    public FinancialGoal execute(UpdateGoalCommand command) {
        // findById já valida a posse: meta de outro usuário sai como 404 daqui.
        FinancialGoal goal = findById(command.goalId(), command.userId());

        goal.update(command.title(),
                new Money(command.targetAmount(), command.currency()),
                command.targetDate());

        return goalRepository.save(goal);
    }

    @Override
    public void execute(UUID goalId, UUID userId) {
        FinancialGoal goal = findById(goalId, userId);
        goalRepository.deleteById(goal.getId());
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
    public FinancialGoal execute(WithdrawCommand command) {
        FinancialGoal goal = findById(command.goalId(), command.userId());

        goal.withdraw(new Money(command.amount(), command.currency()));

        return goalRepository.save(goal);
    }

    @Override
    public FinancialGoal findById(UUID goalId, UUID userId) {
        FinancialGoal goal = goalRepository.findById(goalId)
                .orElseThrow(() -> new ResourceNotFoundException("Meta financeira não encontrada com o ID: " + goalId));

        // Ver ExpenseService#findById: "não encontrado" em vez de "acesso negado", para a rota
        // não virar um oráculo que confirma a existência de metas de outras pessoas.
        if (!goal.isOwnedBy(userId)) {
            throw new ResourceNotFoundException("Meta financeira não encontrada com o ID: " + goalId);
        }

        return goal;
    }

    @Override
    public List<FinancialGoal> listByUser(UUID userId) {
        return goalRepository.findByUserId(userId);
    }
}
