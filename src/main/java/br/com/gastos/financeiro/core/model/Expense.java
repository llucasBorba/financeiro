package br.com.gastos.financeiro.core.model;

import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Um lançamento de saída.
 *
 * <p>O antigo campo {@code type} ({@code FIXED}/{@code VARIABLE}) foi removido: ele nunca
 * alterou comportamento nenhum — era um rótulo que o usuário preenchia e o sistema devolvia.
 * Quem responde "esta despesa se repete?" agora é {@link #getRecurringExpenseId()}: preenchido,
 * a despesa nasceu de uma recorrência; nulo, é um lançamento avulso. Um fato verificável em vez
 * de uma declaração de intenção.
 */
public class Expense {

    private final UUID id;
    private final UUID userId;
    private UUID categoryId;
    private Money amount;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private ExpenseStatus status;   // PENDING ou PAID

    /**
     * Modelo de recorrência que originou este lançamento, ou nulo se foi digitado à mão.
     *
     * <p>É {@code final} porque a origem é um fato do nascimento: uma despesa avulsa não vira
     * recorrente depois, nem o contrário. Editar valor ou vencimento não muda de onde ela veio.
     */
    private final UUID recurringExpenseId;

    public Expense(UUID id, UUID userId, UUID categoryId, Money amount, String description,
                   LocalDate dueDate) {
        this(id, userId, categoryId, amount, description, dueDate, null);
    }

    public Expense(UUID id, UUID userId, UUID categoryId, Money amount, String description,
                   LocalDate dueDate, UUID recurringExpenseId) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.categoryId = Objects.requireNonNull(categoryId, "A categoria é obrigatória.");
        this.amount = Objects.requireNonNull(amount, "O valor da despesa é obrigatório.");
        this.description = description;
        this.dueDate = DateBounds.require(
                Objects.requireNonNull(dueDate, "A data de vencimento é obrigatória."),
                "A data de vencimento");
        this.recurringExpenseId = recurringExpenseId;
        this.status = ExpenseStatus.PENDING;
        this.paidAt = null;
    }

    /**
     * Construtor de reconstituição: usado apenas pela camada de persistência para
     * restaurar uma despesa já existente sem passar de novo pelas regras de transição.
     */
    public static Expense reconstitute(UUID id, UUID userId, UUID categoryId, Money amount, String description,
                                       LocalDate dueDate, ExpenseStatus status, LocalDateTime paidAt,
                                       UUID recurringExpenseId) {
        Expense expense = new Expense(id, userId, categoryId, amount, description, dueDate, recurringExpenseId);
        expense.status = status != null ? status : ExpenseStatus.PENDING;
        expense.paidAt = paidAt;
        return expense;
    }

    /**
     * Corrige os dados de uma despesa já cadastrada.
     *
     * <p>Deliberadamente NÃO toca em {@code status} nem em {@code paidAt}: corrigir um valor
     * digitado errado não deve, como efeito colateral, marcar ou desmarcar um pagamento.
     * Mudar o estado de pagamento continua sendo uma operação separada e explícita
     * ({@link #markAsPaid(LocalDateTime)}), justamente para que seja uma decisão consciente.
     *
     * <p>Também não muda a origem: editar o aluguel de outubro não o desliga da recorrência
     * que o gerou.
     */
    public void update(UUID categoryId, Money amount, String description, LocalDate dueDate) {
        this.amount = Objects.requireNonNull(amount, "O valor da despesa é obrigatório.");
        this.dueDate = DateBounds.require(
                Objects.requireNonNull(dueDate, "A data de vencimento é obrigatória."),
                "A data de vencimento");
        this.categoryId = Objects.requireNonNull(categoryId, "A categoria é obrigatória.");
        this.description = description;
    }

    public void markAsPaid(LocalDateTime paymentDate) {
        if (this.status == ExpenseStatus.PAID) {
            throw new IllegalStateException("Esta despesa já foi paga.");
        }
        this.status = ExpenseStatus.PAID;
        this.paidAt = paymentDate != null
                ? DateBounds.require(paymentDate, "A data de pagamento")
                : LocalDateTime.now();
    }

    /**
     * Devolve a despesa para "a pagar". Simétrico a {@link #markAsPaid(LocalDateTime)}.
     *
     * <p>Sem isto, marcar como paga por engano seria irreversível — só restaria apagar a
     * despesa e recadastrar, perdendo o id e qualquer histórico ligado a ele.
     */
    public void undoPayment() {
        if (this.status == ExpenseStatus.PENDING) {
            throw new IllegalStateException("Esta despesa não está paga.");
        }
        this.status = ExpenseStatus.PENDING;
        this.paidAt = null;
    }

    public boolean isPaid() {
        return this.status == ExpenseStatus.PAID;
    }

    /** Nasceu de uma recorrência? É o que substituiu o antigo {@code type = FIXED}. */
    public boolean isRecurring() {
        return this.recurringExpenseId != null;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    // Getters
    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCategoryId() { return categoryId; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDate getDueDate() { return dueDate; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public ExpenseStatus getStatus() { return status; }
    public UUID getRecurringExpenseId() { return recurringExpenseId; }
}
