package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.ingoing.ChangeCategoryStatusUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.CreateCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RenameCategoryUseCase;
import br.com.gastos.financeiro.core.service.CategoryService;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Decorator transacional dos casos de uso de categoria.
 * Ver {@link TransactionalExpenseService} para o raciocínio por trás desta camada.
 *
 * <p>A exclusão precisa de transação por um motivo específico: ela conta as despesas
 * vinculadas e só então apaga. Sem a transação envolvendo os dois passos, uma despesa
 * poderia ser criada entre a contagem e o DELETE — e aí a chave estrangeira barraria a
 * gravação, mas com um erro de banco cru em vez da nossa mensagem.
 */
public class TransactionalCategoryService implements CreateCategoryUseCase, RenameCategoryUseCase,
        ChangeCategoryStatusUseCase, DeleteCategoryUseCase, FindCategoryUseCase {

    private final CategoryService delegate;

    public TransactionalCategoryService(CategoryService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Category execute(CreateCategoryCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public Category execute(RenameCategoryCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public Category execute(ChangeCategoryStatusCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public void execute(UUID categoryId, UUID userId) {
        delegate.execute(categoryId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Category findById(UUID categoryId, UUID userId) {
        return delegate.findById(categoryId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Category> listByUser(UUID userId, boolean includeInactive) {
        return delegate.listByUser(userId, includeInactive);
    }
}
