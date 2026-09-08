package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.ports.ingoing.CreateIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateIncomeUseCase;
import br.com.gastos.financeiro.core.service.IncomeService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Decorator transacional dos casos de uso de receita.
 * Ver {@link TransactionalExpenseService} para o raciocínio por trás desta camada.
 */
public class TransactionalIncomeService implements CreateIncomeUseCase, UpdateIncomeUseCase,
        DeleteIncomeUseCase, FindIncomeUseCase {

    private final IncomeService delegate;

    public TransactionalIncomeService(IncomeService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Income execute(CreateIncomeCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public Income execute(UpdateIncomeCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public void execute(UUID incomeId, UUID userId) {
        delegate.execute(incomeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Income findById(UUID incomeId, UUID userId) {
        return delegate.findById(incomeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Income> listByUser(UUID userId) {
        return delegate.listByUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Income> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate) {
        return delegate.listByUserAndPeriod(userId, startDate, endDate);
    }
}
