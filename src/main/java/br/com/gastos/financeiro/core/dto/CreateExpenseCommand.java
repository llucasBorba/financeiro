package br.com.gastos.financeiro.core.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseCommand(
        UUID userId,
        UUID categoryId,
        BigDecimal amount,
        String currency,
        String description,
        LocalDate dueDate,
        String type
) {}
