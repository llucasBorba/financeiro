package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.Income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record IncomeResponse(
        UUID id,
        UUID userId,
        BigDecimal amount,
        String currency,
        String description,
        LocalDate receivedAt
) {
    public static IncomeResponse from(Income income) {
        return new IncomeResponse(
                income.getId(),
                income.getUserId(),
                income.getAmount().getAmount(),
                income.getAmount().getCurrency(),
                income.getDescription(),
                income.getReceivedAt());
    }
}
