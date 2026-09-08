package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateGoalUseCase;
import br.com.gastos.financeiro.core.service.FinancialGoalService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Decorator transacional para os casos de uso de meta financeira.
 * Ver {@link TransactionalExpenseService} para o raciocínio por trás desta camada.
 *
 * <p>O aporte é o caso mais crítico: ele lê o saldo, soma na memória e grava de volta.
 * Sem a transação envolvendo os três passos, dois aportes simultâneos leem o mesmo saldo
 * e o segundo sobrescreve o primeiro — o dinheiro some sem erro nenhum.
 */
public class TransactionalFinancialGoalService implements CreateGoalUseCase, UpdateGoalUseCase,
        DeleteGoalUseCase, DepositToGoalUseCase, FindGoalUseCase {

    private final FinancialGoalService delegate;

    public TransactionalFinancialGoalService(FinancialGoalService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public FinancialGoal execute(CreateGoalCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public FinancialGoal execute(UpdateGoalCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public void execute(UUID goalId, UUID userId) {
        delegate.execute(goalId, userId);
    }

    @Override
    @Transactional
    public FinancialGoal execute(DepositCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional(readOnly = true)
    public FinancialGoal findById(UUID goalId, UUID userId) {
        return delegate.findById(goalId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FinancialGoal> listByUser(UUID userId) {
        return delegate.listByUser(userId);
    }
}
