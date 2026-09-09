package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.Budget;

import java.math.BigDecimal;
import java.util.UUID;

public record BudgetResponse(
        UUID categoryId,
        BigDecimal monthlyLimit,
        String currency
) {
    public static BudgetResponse from(Budget budget) {
        return new BudgetResponse(
                budget.getCategoryId(),
                budget.getMonthlyLimit().getAmount(),
                budget.getMonthlyLimit().getCurrency());
    }
}
