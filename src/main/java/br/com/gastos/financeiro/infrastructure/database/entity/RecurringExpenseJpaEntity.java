package br.com.gastos.financeiro.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * {@code YearMonth} não é um tipo que o JPA saiba mapear, então os meses são guardados como
 * {@code LocalDate} no dia 1º. A conversão fica no {@link
 * br.com.gastos.financeiro.infrastructure.database.mapper.RecurringExpenseMapper} — o domínio
 * continua falando em {@code YearMonth}, que é o conceito certo para "mês de competência".
 */
@Entity
@Table(name = "tb_recurring_expenses")
public class RecurringExpenseJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "category_id", nullable = false)
    private UUID categoryId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(length = 255, nullable = false)
    private String description;

    @Column(name = "day_of_month", nullable = false)
    private int dayOfMonth;

    @Column(name = "start_month", nullable = false)
    private LocalDate startMonth;

    @Column(name = "end_month")
    private LocalDate endMonth;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    public RecurringExpenseJpaEntity() {}

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

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public LocalDate getStartMonth() { return startMonth; }
    public void setStartMonth(LocalDate startMonth) { this.startMonth = startMonth; }

    public LocalDate getEndMonth() { return endMonth; }
    public void setEndMonth(LocalDate endMonth) { this.endMonth = endMonth; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Sem setter de propósito: quem controla a versão é o Hibernate. */
    public Long getVersion() { return version; }
}
