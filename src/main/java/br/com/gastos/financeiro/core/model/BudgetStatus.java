package br.com.gastos.financeiro.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Comparação entre o limite de uma categoria e o que aconteceu num mês.
 *
 * <p>Como {@link MonthlySummary}, não é guardado: é derivado dos lançamentos na hora da
 * consulta. Dado derivado gravado envelhece quando a origem muda — pagar uma despesa antiga
 * deixaria o orçamento de três meses atrás mentindo.
 *
 * <p>Separa <strong>gasto</strong> de <strong>a pagar</strong> em vez de escolher uma base, e a
 * razão é prática. Se contasse só o que foi pago: é dia 20, seu limite de Alimentação é
 * R$ 1.000, você tem R$ 900 em contas de mercado no mês e R$ 500 pagas — o app diria 50%, você
 * pagaria o resto e ele saltaria para 90%. O aviso chegaria quando não servisse mais. Se
 * contasse só o vencimento, perderia a coerência com o balanço, que é base caixa por decisão
 * de projeto. Mostrando os dois, o número bate com o resumo mensal <em>e</em> avisa a tempo.
 *
 * <p>As duas definições são exatamente as do resumo mensal, de propósito: {@code spent} é o que
 * foi PAGO dentro do mês, {@code pending} é o que está em aberto vencendo até o fim do mês —
 * incluindo o que venceu antes e não foi pago, porque conta atrasada continua consumindo
 * orçamento até ser quitada.
 */
public class BudgetStatus {

    private final UUID categoryId;
    private final String categoryName;
    private final Money monthlyLimit;
    private final Money spent;
    private final Money pending;

    private BudgetStatus(UUID categoryId, String categoryName, Money monthlyLimit,
                         Money spent, Money pending) {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.monthlyLimit = monthlyLimit;
        this.spent = spent;
        this.pending = pending;
    }

    /**
     * @param paid    despesas cujo PAGAMENTO caiu no mês
     * @param unpaid  despesas em aberto vencendo até o fim do mês
     */
    public static List<BudgetStatus> forMonth(List<Budget> budgets,
                                              List<Expense> paid,
                                              List<Expense> unpaid,
                                              Map<UUID, String> categoryNames) {
        return budgets.stream()
                .map(budget -> {
                    String moeda = budget.getMonthlyLimit().getCurrency();
                    return new BudgetStatus(
                            budget.getCategoryId(),
                            categoryNames.getOrDefault(budget.getCategoryId(), "(categoria removida)"),
                            budget.getMonthlyLimit(),
                            totalOf(paid, budget.getCategoryId(), moeda),
                            totalOf(unpaid, budget.getCategoryId(), moeda));
                })
                // Mais apertado primeiro: quem abre a tela quer ver o que está estourando,
                // não percorrer a lista inteira atrás do problema.
                .sorted(Comparator.comparing(BudgetStatus::projectedPercentage).reversed()
                        .thenComparing(BudgetStatus::getCategoryName))
                .toList();
    }

    /**
     * Soma as despesas de uma categoria, ignorando as de outra moeda.
     *
     * <p>Um limite é declarado numa moeda só. Somar BRL com USD daria um número sem
     * significado, então despesa em moeda diferente simplesmente não pertence a este orçamento.
     */
    private static Money totalOf(List<Expense> despesas, UUID categoryId, String currency) {
        return despesas.stream()
                .filter(d -> categoryId.equals(d.getCategoryId()))
                .map(Expense::getAmount)
                .filter(valor -> valor.getCurrency().equals(currency))
                .reduce(Money.zero(currency), Money::add);
    }

    /** O que já saiu, em porcentagem do limite. */
    public BigDecimal usedPercentage() {
        return percentageOf(spent.getAmount());
    }

    /** O que já saiu somado ao que ainda vai sair. É este número que avisa a tempo. */
    public BigDecimal projectedPercentage() {
        return percentageOf(spent.getAmount().add(pending.getAmount()));
    }

    /**
     * Quanto ainda cabe no limite. Devolve {@link BigDecimal}, e não {@link Money}, porque
     * pode ser negativo — e quantia negativa não existe. Mesma razão de
     * {@link MonthlySummary#balance()}.
     */
    public BigDecimal remaining() {
        return monthlyLimit.getAmount().subtract(spent.getAmount());
    }

    /** Já estourou com dinheiro que de fato saiu. */
    public boolean isExceeded() {
        return spent.getAmount().compareTo(monthlyLimit.getAmount()) > 0;
    }

    /** Vai estourar se tudo que está em aberto for pago. É o aviso antecipado. */
    public boolean isProjectedToExceed() {
        return spent.getAmount().add(pending.getAmount())
                .compareTo(monthlyLimit.getAmount()) > 0;
    }

    private BigDecimal percentageOf(BigDecimal parte) {
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(monthlyLimit.getAmount(), 2, RoundingMode.HALF_UP);
    }

    public UUID getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }
    public Money getMonthlyLimit() { return monthlyLimit; }
    public Money getSpent() { return spent; }
    public Money getPending() { return pending; }
}
