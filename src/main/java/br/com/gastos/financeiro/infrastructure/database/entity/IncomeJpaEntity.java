package br.com.gastos.financeiro.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Índices vivem nas migrações Flyway ({@code V6__create_incomes.sql}), não aqui. */
@Entity
@Table(name = "tb_incomes")
public class IncomeJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(length = 3, nullable = false)
    private String currency;

    /** Obrigatória: sem categoria, é a única identidade do lançamento. */
    @Column(length = 255, nullable = false)
    private String description;

    @Column(name = "received_at", nullable = false)
    private LocalDate receivedAt;

    @Version
    private Long version;

    public IncomeJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal amount) { this.amount = amount; }

    public String getCurrency() { return currency; }
    public void setCurrency(String currency) { this.currency = currency; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getReceivedAt() { return receivedAt; }
    public void setReceivedAt(LocalDate receivedAt) { this.receivedAt = receivedAt; }

    /** Sem setter de propósito: quem controla a versão é o Hibernate. */
    public Long getVersion() { return version; }
}
