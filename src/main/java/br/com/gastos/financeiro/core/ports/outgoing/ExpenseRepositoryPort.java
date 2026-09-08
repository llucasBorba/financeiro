package br.com.gastos.financeiro.core.ports.outgoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExpenseRepositoryPort {

    Expense save(Expense expense);

    Optional<Expense> findById(UUID id);

    List<Expense> findByUserId(UUID userId);

    List<Expense> findByUserIdAndDueDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Todas as despesas que vencem até {@code limit}, inclusive.
     *
     * <p>Base do "a pagar" do resumo: uma conta de agosto não paga continua sendo dívida em
     * setembro, então ela precisa aparecer também nos meses seguintes — e não sumir do radar
     * como aconteceria se a busca fosse só pelo intervalo do mês.
     */
    List<Expense> findByUserIdAndDueDateUpTo(UUID userId, LocalDate limit);

    /**
     * Despesas cujo PAGAMENTO caiu no intervalo — base do "saiu" no resumo mensal.
     *
     * <p>Diferente de {@link #findByUserIdAndDueDateBetween}: aqui o critério é quando o
     * dinheiro saiu, não quando a conta vencia. Despesa pendente tem {@code paidAt} nulo e
     * nunca entra no resultado.
     */
    List<Expense> findByUserIdAndPaidAtBetween(UUID userId, LocalDateTime from, LocalDateTime to);

    /**
     * Ocorrências já geradas por um modelo de recorrência.
     *
     * <p>Serve a três operações: não duplicar mês ao gerar, propagar a edição do modelo para
     * as futuras, e limpar as futuras quando o modelo é apagado.
     */
    List<Expense> findByRecurringExpenseId(UUID recurringExpenseId);

    /**
     * Quantas despesas usam esta categoria.
     *
     * <p>Fica na port de DESPESA, e não na de categoria, porque a pergunta é sobre despesas.
     * É o {@code CategoryService} que a consome, antes de permitir uma exclusão.
     */
    long countByCategoryId(UUID categoryId);

    void deleteById(UUID id);
}

