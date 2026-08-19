package br.com.gastos.financeiro.infrastructure.database.entity;

import br.com.gastos.financeiro.core.model.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "tb_financial_goal")
public class FinancialGoalJpaEntity {

    @Id
    private final UUID id;

    @Column(name = "user_id", nullable = false)
    private final UUID userId;

    private String title;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal targetAmount;

    @Column(length = 3, nullable = false)
    private BigDecimal currentAmount;

    @Column(name = "target_date")
    private LocalDate targetDate;
}
