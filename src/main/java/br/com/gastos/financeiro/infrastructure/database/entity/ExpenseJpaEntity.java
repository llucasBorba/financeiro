package br.com.gastos.financeiro.infrastructure.database.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "tb_expenses")
public class ExpenseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id")
    private UUID categoryId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(length = 255)
    private String description;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    @Column(length = 20, nullable = false)
    private String status; // PENDING ou PAID

    /**
     * Controle de concorrência otimista.
     *
     * <p>O Hibernate lê esta coluna junto com a linha e, no UPDATE, gera
     * {@code ... WHERE id = ? AND version = ?} incrementando o valor. Se outra transação
     * gravou a mesma linha nesse meio-tempo a versão já mudou, o UPDATE não acerta nenhuma
     * linha e o Hibernate lança OptimisticLockException em vez de sobrescrever o dado alheio.
     *
     * <p>Precisa ser {@code Long} (e não {@code long}): o Spring Data usa "version == null"
     * para decidir entre INSERT e UPDATE, e um primitivo nunca seria nulo.
     */
    @Version
    private Long version;

    /** Modelo de recorrência que gerou esta despesa. Nulo = lançamento avulso. */
    @Column(name = "recurring_expense_id")
    private UUID recurringExpenseId;


    public ExpenseJpaEntity() {}



    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }

    public LocalDateTime getPaidAt() { return paidAt; }
    public void setPaidAt(LocalDateTime paidAt) { this.paidAt = paidAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public UUID getRecurringExpenseId() { return recurringExpenseId; }
    public void setRecurringExpenseId(UUID recurringExpenseId) { this.recurringExpenseId = recurringExpenseId; }

    /** Sem setter de propósito: quem controla a versão é o Hibernate, nunca o código de aplicação. */
    public Long getVersion() { return version; }
}
