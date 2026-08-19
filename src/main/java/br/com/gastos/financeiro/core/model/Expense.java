package br.com.gastos.financeiro.core.model;

import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;
import br.com.gastos.financeiro.core.model.enums.ExpenseType;

import java.time.LocalDate;
import java.time.LocalDateTime;
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
        this.userId = userId;
        this.categoryId = categoryId;
        this.amount = amount;
        this.description = description;
        this.dueDate = dueDate;
        this.type = type;
        this.status = ExpenseStatus.PENDING;
        this.paidAt = null;
    }

    public void markAsPaid(LocalDateTime paymentDate) {
        if (this.status == ExpenseStatus.PAID) {
            throw new IllegalStateException("Esta despesa já foi paga.");
        }
        this.status = ExpenseStatus.PAID;
        this.paidAt = paymentDate != null ? paymentDate : LocalDateTime.now();
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
