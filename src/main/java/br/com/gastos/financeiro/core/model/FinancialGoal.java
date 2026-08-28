package br.com.gastos.financeiro.core.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class FinancialGoal {

    private final UUID id;
    private final UUID userId;
    private String title;
    private Money targetAmount;
    private Money currentAmount;
    private LocalDate targetDate;

    public FinancialGoal(UUID id, UUID userId, String title, Money targetAmount, LocalDate targetDate) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.title = Objects.requireNonNull(title, "O título da meta é obrigatório.");
        this.targetAmount = Objects.requireNonNull(targetAmount, "O valor alvo é obrigatório.");
        this.currentAmount = Money.zero(targetAmount.getCurrency());
        this.targetDate = targetDate;
    }

    /**
     * Construtor de reconstituição: usado apenas pela camada de persistência para
     * restaurar uma meta já existente com o saldo que ela tem hoje.
     */
    public static FinancialGoal reconstitute(UUID id, UUID userId, String title, Money targetAmount,
                                             Money currentAmount, LocalDate targetDate) {
        FinancialGoal goal = new FinancialGoal(id, userId, title, targetAmount, targetDate);
        goal.currentAmount = currentAmount != null ? currentAmount : Money.zero(targetAmount.getCurrency());
        return goal;
    }

    // Regra de negócio: adicionar aporte à meta
    public void deposit(Money contribution) {
        Objects.requireNonNull(contribution, "O valor do aporte é obrigatório.");
        if (contribution.isZero()) {
            throw new IllegalArgumentException("O aporte deve ser maior que zero.");
        }
        this.currentAmount = this.currentAmount.add(contribution);
    }

    // Métodos utilitários de negócio
    public boolean isAchieved() {
        return this.currentAmount.getAmount().compareTo(this.targetAmount.getAmount()) >= 0;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public Money getTargetAmount() { return targetAmount; }
    public Money getCurrentAmount() { return currentAmount; }
    public LocalDate getTargetDate() { return targetDate; }
}
