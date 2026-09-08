package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Carrega {@code categoryName} junto com o {@code categoryId} para a resposta se explicar
 * sozinha: um log ou uma chamada no Swagger mostrando só o UUID não diz nada sobre de que
 * categoria se trata. O cliente continua usando o id para referenciar; o nome é para leitura.
 */
public record ExpenseResponse(
        UUID id,
        UUID userId,
        UUID categoryId,
        String categoryName,
        BigDecimal amount,
        String currency,
        String description,
        LocalDate dueDate,
        String status,
        LocalDateTime paidAt,

        @io.swagger.v3.oas.annotations.media.Schema(
                description = "Modelo de recorrência que gerou esta despesa. Nulo = lançamento avulso. "
                        + "Substituiu o antigo campo 'type'.")
        UUID recurringExpenseId
) {
    public static ExpenseResponse from(Expense expense, String categoryName) {
        return new ExpenseResponse(
                expense.getId(),
                expense.getUserId(),
                expense.getCategoryId(),
                categoryName,
                expense.getAmount().getAmount(),
                expense.getAmount().getCurrency(),
                expense.getDescription(),
                expense.getDueDate(),
                expense.getStatus().name(),
                expense.getPaidAt(),
                expense.getRecurringExpenseId()
        );
    }
}
