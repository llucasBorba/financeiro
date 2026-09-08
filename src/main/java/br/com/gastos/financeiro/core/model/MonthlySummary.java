package br.com.gastos.financeiro.core.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * O fechamento de um mês: quanto entrou, quanto saiu, quanto ainda se deve e para onde foi.
 *
 * <p><strong>Regra de seleção</strong> — a decisão de modelagem mais importante aqui:
 * <ul>
 *   <li><em>Entrou</em>: receitas recebidas dentro do mês.</li>
 *   <li><em>Saiu</em>: despesas <strong>pagas</strong> dentro do mês, não importa quando
 *       venceram. É regime de caixa: reflete o dinheiro que de fato deixou a conta.</li>
 *   <li><em>A pagar</em>: despesas que <strong>vencem</strong> no mês e continuam pendentes.
 *       É informativo e <strong>fica fora do saldo</strong> — você ainda não gastou.</li>
 * </ul>
 *
 * <p>A propriedade boa disso é não haver contagem dupla: a conta que venceu em setembro e foi
 * paga em outubro aparece como "a pagar" em setembro enquanto pendente, some de lá ao ser paga,
 * e entra como saída de outubro. Cada real é contado uma vez só, no mês em que se moveu.
 */
public class MonthlySummary {

    /** Uma linha da quebra "onde o dinheiro foi". */
    public record CategoryTotal(UUID categoryId, String categoryName, Money total, BigDecimal percentage) {}

    private final YearMonth month;
    private final String currency;
    private final Money received;
    private final Money paid;
    private final Money pending;
    private final List<CategoryTotal> byCategory;
    private final int receivedCount;
    private final int paidCount;
    private final int pendingCount;

    private MonthlySummary(YearMonth month, String currency, Money received, Money paid, Money pending,
                           List<CategoryTotal> byCategory, int receivedCount, int paidCount, int pendingCount) {
        this.month = month;
        this.currency = currency;
        this.received = received;
        this.paid = paid;
        this.pending = pending;
        this.byCategory = List.copyOf(byCategory);
        this.receivedCount = receivedCount;
        this.paidCount = paidCount;
        this.pendingCount = pendingCount;
    }

    /**
     * Monta um resumo por moeda encontrada nos lançamentos.
     *
     * <p>Somar moedas diferentes não faz sentido, e o {@link Money} recusa a operação. Em vez de
     * escolher uma moeda "principal" e esconder o resto — recriando o buraco invisível que
     * evitamos ao tornar a categoria obrigatória —, cada moeda ganha seu próprio fechamento.
     * Na prática a lista quase sempre tem um item só.
     *
     * <p>Mês sem nenhum lançamento devolve um resumo zerado, não uma lista vazia: "setembro
     * fechou em R$ 0,00" é uma resposta; nada não é.
     */
    public static List<MonthlySummary> forMonth(YearMonth month,
                                                List<Income> incomes,
                                                List<Expense> paidExpenses,
                                                List<Expense> pendingExpenses,
                                                Map<UUID, String> categoryNames) {
        Set<String> currencies = new LinkedHashSet<>();
        incomes.forEach(i -> currencies.add(i.getAmount().getCurrency()));
        paidExpenses.forEach(e -> currencies.add(e.getAmount().getCurrency()));
        pendingExpenses.forEach(e -> currencies.add(e.getAmount().getCurrency()));

        if (currencies.isEmpty()) {
            currencies.add(Money.DEFAULT_CURRENCY);
        }

        return currencies.stream()
                .sorted()
                .map(currency -> of(month, currency,
                        filterByCurrency(incomes, Income::getAmount, currency),
                        filterByCurrency(paidExpenses, Expense::getAmount, currency),
                        filterByCurrency(pendingExpenses, Expense::getAmount, currency),
                        categoryNames))
                .toList();
    }

    private static MonthlySummary of(YearMonth month, String currency,
                                     List<Income> incomes, List<Expense> paid, List<Expense> pending,
                                     Map<UUID, String> categoryNames) {
        Money received = sum(incomes.stream().map(Income::getAmount).toList(), currency);
        Money paidTotal = sum(paid.stream().map(Expense::getAmount).toList(), currency);
        Money pendingTotal = sum(pending.stream().map(Expense::getAmount).toList(), currency);

        return new MonthlySummary(month, currency, received, paidTotal, pendingTotal,
                breakdown(paid, paidTotal, currency, categoryNames),
                incomes.size(), paid.size(), pending.size());
    }

    /**
     * A quebra por categoria segue o que <em>saiu</em>, não o que vence: ela existe para
     * explicar o saldo, e o saldo é feito do que foi pago.
     */
    private static List<CategoryTotal> breakdown(List<Expense> paid, Money paidTotal, String currency,
                                                 Map<UUID, String> categoryNames) {
        Map<UUID, List<Expense>> porCategoria = new java.util.LinkedHashMap<>();
        paid.forEach(e -> porCategoria.computeIfAbsent(e.getCategoryId(), k -> new java.util.ArrayList<>()).add(e));

        return porCategoria.entrySet().stream()
                .map(entrada -> {
                    Money total = sum(entrada.getValue().stream().map(Expense::getAmount).toList(), currency);
                    return new CategoryTotal(
                            entrada.getKey(),
                            categoryNames.getOrDefault(entrada.getKey(), "(categoria removida)"),
                            total,
                            percentageOf(total, paidTotal));
                })
                // maior gasto primeiro: é a informação que a pessoa procura ao abrir o resumo
                .sorted(Comparator.comparing((CategoryTotal c) -> c.total().getAmount()).reversed())
                .toList();
    }

    private static BigDecimal percentageOf(Money parte, Money todo) {
        if (todo.isZero()) {
            return BigDecimal.ZERO.setScale(1);
        }
        return parte.getAmount()
                .multiply(BigDecimal.valueOf(100))
                .divide(todo.getAmount(), 1, RoundingMode.HALF_UP);
    }

    private static <T> List<T> filterByCurrency(List<T> itens,
                                                java.util.function.Function<T, Money> valor,
                                                String currency) {
        return itens.stream().filter(i -> valor.apply(i).getCurrency().equals(currency)).toList();
    }

    private static Money sum(List<Money> valores, String currency) {
        return valores.stream().reduce(Money.zero(currency), Money::add);
    }

    /**
     * Entrou menos saiu. <strong>Pode ser negativo</strong> — daí ser {@link BigDecimal} e não
     * {@link Money}.
     *
     * <p>O {@code Money} proíbe valor negativo por construção, e essa invariante é acertada onde
     * ele é usado: não existe despesa de −50 nem meta com saldo negativo. Mas um saldo mensal
     * negativo é um fato legítimo — significa que você gastou mais do que ganhou, e o sistema
     * precisa saber dizer isso. Forçar o {@code Money} aqui exigiria afrouxar a regra em todos os
     * outros lugares para atender um único caso.
     *
     * <p>A moeda não se perde: ela está em {@link #getCurrency()}, uma vez para o resumo inteiro.
     */
    public BigDecimal balance() {
        return received.getAmount().subtract(paid.getAmount());
    }

    /** Saldo se todas as contas que vencem no mês forem pagas. Também pode ser negativo. */
    public BigDecimal projectedBalance() {
        return balance().subtract(pending.getAmount());
    }

    public YearMonth getMonth() { return month; }
    public String getCurrency() { return currency; }
    public Money getReceived() { return received; }
    public Money getPaid() { return paid; }
    public Money getPending() { return pending; }
    public List<CategoryTotal> getByCategory() { return byCategory; }
    public int getReceivedCount() { return receivedCount; }
    public int getPaidCount() { return paidCount; }
    public int getPendingCount() { return pendingCount; }
}
