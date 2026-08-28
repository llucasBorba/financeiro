package br.com.gastos.financeiro.core.model;

import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;
import br.com.gastos.financeiro.core.model.enums.ExpenseType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Expense {
    private final UUID id;
    private final UUID userId;
    private UUID categoryId;
    private Money amount;
    private String description;
    private LocalDate dueDate;
    private LocalDateTime paidAt;
    private final ExpenseType type; // FIXED ou VARIABLE
    private ExpenseStatus status;   // PENDING ou PAID

    public Expense(UUID id, UUID userId, UUID categoryId, Money amount, String description, LocalDate dueDate, ExpenseType type) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.categoryId = categoryId;
        this.amount = Objects.requireNonNull(amount, "O valor da despesa é obrigatório.");
        this.description = description;
        this.dueDate = Objects.requireNonNull(dueDate, "A data de vencimento é obrigatória.");
        this.type = Objects.requireNonNull(type, "O tipo da despesa é obrigatório.");
        this.status = ExpenseStatus.PENDING;
        this.paidAt = null;
    }

    /**
     * Construtor de reconstituição: usado apenas pela camada de persistência para
     * restaurar uma despesa já existente sem passar de novo pelas regras de transição.
     */
    public static Expense reconstitute(UUID id, UUID userId, UUID categoryId, Money amount, String description,
                                       LocalDate dueDate, ExpenseType type, ExpenseStatus status, LocalDateTime paidAt) {
        Expense expense = new Expense(id, userId, categoryId, amount, description, dueDate, type);
        expense.status = status != null ? status : ExpenseStatus.PENDING;
        expense.paidAt = paidAt;
        return expense;
    }

    public void markAsPaid(LocalDateTime paymentDate) {
        if (this.status == ExpenseStatus.PAID) {
            throw new IllegalStateException("Esta despesa já foi paga.");
        }
        this.status = ExpenseStatus.PAID;
        this.paidAt = paymentDate != null ? paymentDate : LocalDateTime.now();
    }

    public boolean isPaid() {
        return this.status == ExpenseStatus.PAID;
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
    public ExpenseType getType() { return type; }
    public ExpenseStatus getStatus() { return status; }
}
