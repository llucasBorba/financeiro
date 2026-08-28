package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.FinancialGoal;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record GoalResponse(
        UUID id,
        UUID userId,
        String title,
        BigDecimal targetAmount,
        BigDecimal currentAmount,
        String currency,
        LocalDate targetDate,
        boolean achieved
) {
    public static GoalResponse from(FinancialGoal goal) {
        return new GoalResponse(
                goal.getId(),
                goal.getUserId(),
                goal.getTitle(),
                goal.getTargetAmount().getAmount(),
                goal.getCurrentAmount().getAmount(),
                goal.getTargetAmount().getCurrency(),
                goal.getTargetDate(),
                goal.isAchieved()
        );
    }
}
