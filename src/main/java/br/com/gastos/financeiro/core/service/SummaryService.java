package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.model.MonthlySummary;
import br.com.gastos.financeiro.core.ports.ingoing.GetMonthlySummaryUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.IncomeRepositoryPort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Monta o fechamento do mês a partir dos lançamentos.
 *
 * <p>É o primeiro serviço que atravessa <strong>três</strong> agregados: despesa, receita e
 * categoria. Ainda assim ele não guarda nada — o resumo é derivado, calculado na hora. Um mês
 * tem ~100 lançamentos; carregar e somar em memória é instantâneo, e evita a pior classe de bug
 * de dado derivado: o valor gravado que envelhece quando o dado de origem muda.
 *
 * <p>Se um dia o volume justificar, o caminho é uma port de leitura com {@code SUM/GROUP BY}
 * no SQL. O gatilho é claro: quando um período passar de alguns milhares de linhas.
 */
public class SummaryService implements GetMonthlySummaryUseCase {

    private final ExpenseRepositoryPort expenseRepository;
    private final IncomeRepositoryPort incomeRepository;
    private final CategoryRepositoryPort categoryRepository;

    public SummaryService(ExpenseRepositoryPort expenseRepository,
                          IncomeRepositoryPort incomeRepository,
                          CategoryRepositoryPort categoryRepository) {
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "O repositório de despesas é obrigatório.");
        this.incomeRepository = Objects.requireNonNull(incomeRepository, "O repositório de receitas é obrigatório.");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "O repositório de categorias é obrigatório.");
    }

    @Override
    public List<MonthlySummary> execute(UUID userId, YearMonth month) {
        LocalDate primeiroDia = month.atDay(1);
        LocalDate ultimoDia = month.atEndOfMonth();

        List<Income> recebidas = incomeRepository.findByUserIdAndReceivedAtBetween(userId, primeiroDia, ultimoDia);

        // "Saiu" é pelo PAGAMENTO: o dinheiro que deixou a conta neste mês, não importa
        // quando a conta vencia.
        List<Expense> pagas = expenseRepository.findByUserIdAndPaidAtBetween(
                userId, primeiroDia.atStartOfDay(), ultimoDia.atTime(LocalTime.MAX));

        // "A pagar" é pelo VENCIMENTO, e inclui o que venceu ANTES e segue pendente: uma conta
        // de agosto não paga continua sendo dívida em setembro. Buscar só o intervalo do mês
        // faria ela sumir do radar justamente quando vira problema.
        //
        // Quando a conta for paga ela some daqui e reaparece como saída do mês em que foi quitada.
        List<Expense> aPagar = expenseRepository.findByUserIdAndDueDateUpTo(userId, ultimoDia)
                .stream()
                .filter(despesa -> !despesa.isPaid())
                .toList();

        // Inclui arquivadas: uma despesa antiga pode apontar para categoria já arquivada,
        // e o nome dela ainda precisa aparecer no histórico.
        Map<UUID, String> nomes = categoryRepository.findByUserId(userId, true).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return MonthlySummary.forMonth(month, recebidas, pagas, aPagar, nomes);
    }
}
