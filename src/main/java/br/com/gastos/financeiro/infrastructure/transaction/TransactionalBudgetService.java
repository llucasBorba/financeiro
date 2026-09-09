package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.Budget;
import br.com.gastos.financeiro.core.model.BudgetStatus;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteBudgetUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.ListBudgetStatusUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.SetBudgetUseCase;
import br.com.gastos.financeiro.core.service.BudgetService;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Decorator transacional para os casos de uso de orçamento.
 * Ver {@link TransactionalExpenseService} para o raciocínio por trás desta camada.
 *
 * <p>A gravação é busca-e-atualiza, então precisa da transação pelo mesmo motivo do aporte:
 * sem ela, duas requisições simultâneas passariam ambas pela busca antes de qualquer uma
 * gravar, e criariam dois orçamentos para a mesma categoria. O índice único do banco recusaria
 * a segunda — mas com a transação o {@code @Version} resolve antes de chegar lá.
 *
 * <p>A leitura é {@code readOnly}: são três consultas (orçamentos, pagas, a pagar) que precisam
 * enxergar o mesmo instante. Sem transação única, uma despesa paga no meio da execução
 * apareceria em uma consulta e não na outra, e os números não fechariam entre si.
 */
public class TransactionalBudgetService implements SetBudgetUseCase, ListBudgetStatusUseCase, DeleteBudgetUseCase {

    private final BudgetService delegate;

    public TransactionalBudgetService(BudgetService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Budget execute(SetBudgetCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BudgetStatus> execute(UUID userId, YearMonth month) {
        return delegate.execute(userId, month);
    }

    @Override
    @Transactional
    public void execute(UUID userId, UUID categoryId) {
        delegate.execute(userId, categoryId);
    }
}
