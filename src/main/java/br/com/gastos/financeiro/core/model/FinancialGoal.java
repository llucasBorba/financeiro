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
            this.currentAmount = new Money(java.math.BigDecimal.ZERO, targetAmount.getCurrency());
            this.targetDate = targetDate;
        }

        // Regra de negócio: adicionar aporte à meta
        public void deposit(Money contribution) {
            this.currentAmount = this.currentAmount.add(contribution);
        }

        // Métodos utilitários de negócio
        public boolean isAchieved() {
            return this.currentAmount.getAmount().compareTo(this.targetAmount.getAmount()) >= 0;
        }

        public UUID getId() { return id; }
        public UUID getUserId() { return userId; }
        public String getTitle() { return title; }
        public Money getTargetAmount() { return targetAmount; }
        public Money getCurrentAmount() { return currentAmount; }
        public LocalDate getTargetDate() { return targetDate; }
    }
