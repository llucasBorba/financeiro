package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record ExpenseResponse(
        UUID id,
        UUID userId,
        UUID categoryId,
        BigDecimal amount,
        String currency,
        String description,
        LocalDate dueDate,
        String type,
        String status,
        LocalDateTime paidAt
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getUserId(),
                expense.getCategoryId(),
                expense.getAmount().getAmount(),
                expense.getAmount().getCurrency(),
                expense.getDescription(),
                expense.getDueDate(),
                expense.getType().name(),
                expense.getStatus().name(),
                expense.getPaidAt()
        );
    }
}
