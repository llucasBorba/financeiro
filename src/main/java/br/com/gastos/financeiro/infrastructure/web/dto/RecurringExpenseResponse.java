package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.RecurringExpense;

import java.math.BigDecimal;
import java.util.UUID;

public record RecurringExpenseResponse(
        UUID id,
        UUID userId,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        String currency,
        String description,
        int dayOfMonth,
        String startMonth,
        String endMonth,
        boolean active
) {
    public static RecurringExpenseResponse from(RecurringExpense modelo, String categoryName) {
        return new RecurringExpenseResponse(
                modelo.getId(),
                modelo.getUserId(),
                modelo.getCategoryId(),
                categoryName,
                modelo.getAmount().getAmount(),
                modelo.getAmount().getCurrency(),
                modelo.getDescription(),
                modelo.getDayOfMonth(),
                modelo.getStartMonth().toString(),
                modelo.getEndMonth() != null ? modelo.getEndMonth().toString() : null,
                modelo.isActive());
    }
}
