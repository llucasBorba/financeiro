package br.com.gastos.financeiro.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_financial_goals")
public class FinancialGoalJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "target_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(name = "current_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal currentAmount;

    @Column(length = 3, nullable = false)
    private String currency;

    @Column(name = "target_date")
    private LocalDate targetDate;

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

    public FinancialGoalJpaEntity() {}

    public FinancialGoalJpaEntity(UUID id, UUID userId, String title, BigDecimal targetAmount,
                                  BigDecimal currentAmount, String currency, LocalDate targetDate) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.targetAmount = targetAmount;
        this.currentAmount = currentAmount;
        this.currency = currency;
        this.targetDate = targetDate;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public BigDecimal getTargetAmount() { return targetAmount; }
    public void setTargetAmount(BigDecimal targetAmount) { this.targetAmount = targetAmount; }

    public BigDecimal getCurrentAmount() { return currentAmount; }
    public void setCurrentAmount(BigDecimal currentAmount) { this.currentAmount = currentAmount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    /** Sem setter de propósito: quem controla a versão é o Hibernate, nunca o código de aplicação. */
    public Long getVersion() { return version; }
}
