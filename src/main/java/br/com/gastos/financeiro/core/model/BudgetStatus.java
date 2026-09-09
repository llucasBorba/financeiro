package br.com.gastos.financeiro.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * O que aconteceu com uma categoria num mês, confrontado com o limite dela — quando há limite.
 *
 * <p>Como {@link MonthlySummary}, não é guardado: é derivado dos lançamentos na hora da
 * consulta. Dado derivado gravado envelhece quando a origem muda — pagar uma despesa antiga
 * deixaria o orçamento de três meses atrás mentindo.
 *
 * <p><b>Categoria sem limite também aparece</b>, com {@code monthlyLimit} nulo e os gastos
 * preenchidos. Esconder essas linhas deixaria de fora justamente o gasto que ninguém está
 * controlando: a categoria em que o dinheiro está indo embora sem que exista um teto para
 * avisar é a mais importante de enxergar, não a menos.
 *
 * <p>Sem limite, tudo que se calcula <em>a partir</em> do limite é nulo, não zero. Devolver
 * "0% usado" para quem gastou R$ 800 seria mentir com um número que parece verdadeiro;
 * porcentagem de um limite que não existe é indefinida, e {@code null} é o que diz isso.
 * {@link #hasBudget()} existe para o cliente não precisar deduzir isso de um campo nulo.
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

    /**
     * Uma linha do resultado é uma categoria <em>numa moeda</em>, não só uma categoria.
     *
     * <p>Um limite é declarado numa moeda só, e somar BRL com USD daria um número sem
     * significado. Com a chave composta, cada despesa entra em exatamente uma linha e nenhuma
     * é descartada em silêncio: uma categoria com limite em BRL que tenha gasto em USD produz
     * a linha do limite e mais uma linha sem limite para o gasto em USD.
     */
    private record Key(UUID categoryId, String currency) {}

    private final UUID categoryId;
    private final String categoryName;

    /** Nulo quando a categoria teve movimento mas nenhum limite foi definido. */
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

        Map<Key, Money> limites = new LinkedHashMap<>();
        budgets.forEach(b -> limites.put(
                new Key(b.getCategoryId(), b.getMonthlyLimit().getCurrency()), b.getMonthlyLimit()));

        // A lista é a UNIÃO: tudo que tem limite, mais tudo que teve movimento no mês.
        Set<Key> linhas = new LinkedHashSet<>(limites.keySet());
        Stream.concat(paid.stream(), unpaid.stream())
                .forEach(e -> linhas.add(new Key(e.getCategoryId(), e.getAmount().getCurrency())));

        return linhas.stream()
                .map(chave -> new BudgetStatus(
                        chave.categoryId(),
                        categoryNames.getOrDefault(chave.categoryId(), "(categoria removida)"),
                        limites.get(chave),
                        totalOf(paid, chave),
                        totalOf(unpaid, chave)))
                .sorted(ORDEM)
                .toList();
    }

    /**
     * Categorias com limite primeiro, da mais apertada para a mais folgada; depois as sem
     * limite, da que mais consumiu para a que menos.
     *
     * <p>Quem abre a tela quer ver primeiro o que está estourando um teto que ele mesmo
     * definiu. O gasto sem teto vem logo abaixo, ainda no topo da lista, porque é a próxima
     * pergunta natural: "e onde mais o dinheiro está indo?".
     */
    private static final Comparator<BudgetStatus> ORDEM =
            Comparator.comparingInt((BudgetStatus s) -> s.hasBudget() ? 0 : 1)
                    // Dentro de cada grupo a métrica é a mesma, então a comparação é coerente:
                    // porcentagem entre as com limite, valor gasto entre as sem.
                    .thenComparing(Comparator.comparing(BudgetStatus::urgency).reversed())
                    .thenComparing(BudgetStatus::getCategoryName);

    private BigDecimal urgency() {
        return hasBudget() ? projectedPercentage() : spent.getAmount().add(pending.getAmount());
    }

    private static Money totalOf(List<Expense> despesas, Key chave) {
        return despesas.stream()
                .filter(d -> chave.categoryId().equals(d.getCategoryId()))
                .map(Expense::getAmount)
                .filter(valor -> valor.getCurrency().equals(chave.currency()))
                .reduce(Money.zero(chave.currency()), Money::add);
    }

    /** Se existe um limite definido para esta categoria. Quando falso, tudo abaixo é nulo. */
    public boolean hasBudget() {
        return monthlyLimit != null;
    }

    /** O que já saiu, em porcentagem do limite. Nulo sem limite. */
    public BigDecimal usedPercentage() {
        return hasBudget() ? percentageOf(spent.getAmount()) : null;
    }

    /** O que já saiu somado ao que ainda vai sair. É este número que avisa a tempo. Nulo sem limite. */
    public BigDecimal projectedPercentage() {
        return hasBudget() ? percentageOf(spent.getAmount().add(pending.getAmount())) : null;
    }

    /**
     * Quanto ainda cabe no limite; nulo sem limite. Devolve {@link BigDecimal}, e não
     * {@link Money}, porque pode ser negativo — e quantia negativa não existe. Mesma razão de
     * {@link MonthlySummary#balance()}.
     */
    public BigDecimal remaining() {
        return hasBudget() ? monthlyLimit.getAmount().subtract(spent.getAmount()) : null;
    }

    /** Já estourou com dinheiro que de fato saiu. Falso sem limite: não há teto a estourar. */
    public boolean isExceeded() {
        return hasBudget() && spent.getAmount().compareTo(monthlyLimit.getAmount()) > 0;
    }

    /** Vai estourar se tudo que está em aberto for pago. É o aviso antecipado. */
    public boolean isProjectedToExceed() {
        return hasBudget()
                && spent.getAmount().add(pending.getAmount()).compareTo(monthlyLimit.getAmount()) > 0;
    }

    private BigDecimal percentageOf(BigDecimal parte) {
        return parte.multiply(BigDecimal.valueOf(100))
                .divide(monthlyLimit.getAmount(), 2, RoundingMode.HALF_UP);
    }

    public UUID getCategoryId() { return categoryId; }
    public String getCategoryName() { return categoryName; }

    /** Nulo quando não há limite definido — ver {@link #hasBudget()}. */
    public Money getMonthlyLimit() { return monthlyLimit; }

    public Money getSpent() { return spent; }
    public Money getPending() { return pending; }

    /** A moeda da linha. Vem do limite quando existe, e dos lançamentos quando não. */
    public String getCurrency() {
        return hasBudget() ? monthlyLimit.getCurrency() : spent.getCurrency();
    }
}
