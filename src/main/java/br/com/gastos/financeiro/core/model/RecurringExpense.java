package br.com.gastos.financeiro.core.model;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * A regra de uma despesa que se repete: "aluguel, R$ 1.500, todo dia 10, a partir de out/2026".
 *
 * <p>Este objeto guarda apenas a <strong>regra</strong>. As ocorrências são despesas de verdade
 * em {@code tb_expenses}, geradas a partir dele — e por serem despesas comuns, listagem, resumo
 * mensal e pagamento funcionam sem saber que recorrência existe.
 *
 * <p>Cada ocorrência tem vida própria depois de gerada: pode ser paga numa data, ter o valor
 * corrigido, ou ser apagada — sem afetar as outras nem o modelo.
 */
public class RecurringExpense {

    /** Quantos meses gerar de uma vez quando não há fim previsto. */
    public static final int DEFAULT_HORIZON_MONTHS = 12;

    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private final UUID id;
    private final UUID userId;
    private UUID categoryId;
    private Money amount;
    private String description;
    private int dayOfMonth;
    private final YearMonth startMonth;
    private YearMonth endMonth;
    private boolean active;

    private RecurringExpense(UUID id, UUID userId, UUID categoryId, Money amount, String description,
                             int dayOfMonth, YearMonth startMonth, YearMonth endMonth, boolean active) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.startMonth = Objects.requireNonNull(startMonth, "O mês inicial é obrigatório.");
        this.active = active;

        applyChanges(categoryId, amount, description, dayOfMonth, endMonth);
    }

    public static RecurringExpense create(UUID userId, UUID categoryId, Money amount, String description,
                                          int dayOfMonth, YearMonth startMonth, YearMonth endMonth) {
        return new RecurringExpense(null, userId, categoryId, amount, description,
                dayOfMonth, startMonth, endMonth, true);
    }

    public static RecurringExpense reconstitute(UUID id, UUID userId, UUID categoryId, Money amount,
                                                String description, int dayOfMonth, YearMonth startMonth,
                                                YearMonth endMonth, boolean active) {
        return new RecurringExpense(id, userId, categoryId, amount, description,
                dayOfMonth, startMonth, endMonth, active);
    }

    /** O mês inicial não muda: ele define a origem da série. */
    public void update(UUID categoryId, Money amount, String description, int dayOfMonth, YearMonth endMonth) {
        applyChanges(categoryId, amount, description, dayOfMonth, endMonth);
    }

    private void applyChanges(UUID categoryId, Money amount, String description,
                              int dayOfMonth, YearMonth endMonth) {
        this.categoryId = Objects.requireNonNull(categoryId, "A categoria é obrigatória.");
        this.amount = Objects.requireNonNull(amount, "O valor é obrigatório.");
        this.description = normalizeDescription(description);

        if (dayOfMonth < 1 || dayOfMonth > 31) {
            throw new IllegalArgumentException("O dia do vencimento deve estar entre 1 e 31.");
        }
        this.dayOfMonth = dayOfMonth;

        if (endMonth != null && endMonth.isBefore(startMonth)) {
            throw new IllegalArgumentException("O mês final não pode ser anterior ao inicial.");
        }
        this.endMonth = endMonth;
    }

    /**
     * A data de vencimento neste mês.
     *
     * <p>Aqui mora a regra do dia 31: meses mais curtos <strong>grudam no último dia</strong>.
     * Vencimento 31 cai em 28/02 (ou 29 em ano bissexto), 30/04, 30/06. É o que bancos e
     * cartões fazem — pular o mês deixaria a conta sem cobrança, e empurrar para o dia 1º do
     * mês seguinte jogaria a despesa para a competência errada.
     */
    public LocalDate dueDateFor(YearMonth month) {
        return month.atDay(Math.min(dayOfMonth, month.lengthOfMonth()));
    }

    /** A regra vale neste mês? */
    public boolean covers(YearMonth month) {
        if (!active) return false;
        if (month.isBefore(startMonth)) return false;
        return endMonth == null || !month.isAfter(endMonth);
    }

    /**
     * Meses cobertos pela regra, do início até {@code target} — ou até o fim previsto,
     * o que vier primeiro.
     */
    public List<YearMonth> monthsThrough(YearMonth target) {
        YearMonth ultimo = (endMonth != null && endMonth.isBefore(target)) ? endMonth : target;

        List<YearMonth> meses = new ArrayList<>();
        for (YearMonth mes = startMonth; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            meses.add(mes);
        }
        return meses;
    }

    /** Até onde gerar por padrão, quando ninguém informa um alvo. */
    public YearMonth defaultHorizon() {
        YearMonth padrao = startMonth.plusMonths(DEFAULT_HORIZON_MONTHS - 1L);
        return (endMonth != null && endMonth.isBefore(padrao)) ? endMonth : padrao;
    }

    /**
     * Produz a ocorrência deste mês. É o modelo que sabe montar a despesa — não o serviço.
     */
    public Expense occurrenceFor(YearMonth month) {
        return new Expense(null, userId, categoryId, amount, description, dueDateFor(month), id);
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    private static String normalizeDescription(String description) {
        Objects.requireNonNull(description, "A descrição é obrigatória.");
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "A descrição deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCategoryId() { return categoryId; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public int getDayOfMonth() { return dayOfMonth; }
    public YearMonth getStartMonth() { return startMonth; }
    public YearMonth getEndMonth() { return endMonth; }
    public boolean isActive() { return active; }
}
