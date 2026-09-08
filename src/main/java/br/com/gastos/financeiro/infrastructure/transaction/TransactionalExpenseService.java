package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UndoExpensePaymentUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateExpenseUseCase;
import br.com.gastos.financeiro.core.service.ExpenseService;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Decorator que envolve cada caso de uso de despesa numa transação.
 *
 * <p>Existe para que o pacote {@code core} continue sem nenhuma anotação do Spring. O domínio
 * define <em>o que</em> acontece; esta classe, que já é infraestrutura, decide <em>com quais
 * garantias transacionais</em> aquilo acontece.
 *
 * <p>Sem esta camada, {@code findById} e {@code save} dentro de
 * {@link ExpenseService#execute(MarkAsPaidCommand)} rodariam em transações separadas: duas
 * requisições concorrentes conseguiriam passar juntas pela checagem "esta despesa já foi paga?".
 * Com a transação envolvendo o ciclo inteiro, a coluna {@code version} da entidade JPA barra
 * a segunda gravação.
 */
public class TransactionalExpenseService implements CreateExpenseUseCase, UpdateExpenseUseCase,
        DeleteExpenseUseCase, MarkExpenseAsPaidUseCase, UndoExpensePaymentUseCase, FindExpenseUseCase {

    private final ExpenseService delegate;

    public TransactionalExpenseService(ExpenseService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public Expense execute(CreateExpenseCommand command) {
        return delegate.execute(command);
    }

    /* Edição também é ler-modificar-gravar: precisa da transação para o @Version proteger. */
    @Override
    @Transactional
    public Expense execute(UpdateExpenseCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public void execute(UUID expenseId, UUID userId) {
        delegate.execute(expenseId, userId);
    }

    @Override
    @Transactional
    public Expense execute(MarkAsPaidCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional
    public Expense execute(UndoPaymentCommand command) {
        return delegate.execute(command);
    }

    /*
     * readOnly = true avisa o Hibernate de que nada será gravado: ele pula a verificação
     * de alterações (dirty checking) no fim da transação e o driver pode otimizar a conexão.
     */

    @Override
    @Transactional(readOnly = true)
    public Expense findById(UUID expenseId, UUID userId) {
        return delegate.findById(expenseId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> listByUser(UUID userId) {
        return delegate.listByUser(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Expense> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate) {
        return delegate.listByUserAndPeriod(userId, startDate, endDate);
    }
}
