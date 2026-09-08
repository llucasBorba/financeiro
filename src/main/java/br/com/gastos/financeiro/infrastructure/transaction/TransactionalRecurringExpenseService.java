package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.RecurringExpense;
import br.com.gastos.financeiro.core.ports.ingoing.CreateRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.GenerateOccurrencesUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.service.RecurringExpenseService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Decorator transacional da recorrência — e aqui a transação importa mais que nunca.
 *
 * <p>Criar um modelo grava o modelo <em>e</em> 12 despesas. Apagá-lo remove ocorrências futuras
 * <em>e</em> o modelo. Editá-lo altera o modelo <em>e</em> propaga para as futuras. Sem a
 * transação, uma falha no meio deixaria o sistema num estado sem sentido: modelo sem
 * ocorrências, ou ocorrências órfãs apontando para um modelo que já não existe.
 */
public class TransactionalRecurringExpenseService implements CreateRecurringExpenseUseCase,
        UpdateRecurringExpenseUseCase, DeleteRecurringExpenseUseCase,
        GenerateOccurrencesUseCase, FindRecurringExpenseUseCase {

    private final RecurringExpenseService delegate;

    public TransactionalRecurringExpenseService(RecurringExpenseService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public RecurringExpense execute(CreateRecurringExpenseCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public RecurringExpense execute(UpdateRecurringExpenseCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public List<Expense> execute(GenerateOccurrencesCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public void execute(UUID recurringExpenseId, UUID userId) {
        delegate.execute(recurringExpenseId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public RecurringExpense findById(UUID recurringExpenseId, UUID userId) {
        return delegate.findById(recurringExpenseId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecurringExpense> listByUser(UUID userId) {
        return delegate.listByUser(userId);
    }
}
