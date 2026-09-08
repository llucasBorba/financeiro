package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.UpdateExpenseUseCase.UpdateExpenseCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Corpo do PUT: os mesmos campos do cadastro, menos os que não se editam.
 *
 * <p>Não tem {@code status} nem {@code paidAt} de propósito — o estado de pagamento muda por
 * {@code PATCH /{id}/payment}, nunca como efeito colateral de uma correção de digitação.
 */
public record UpdateExpenseRequest(

        @Schema(description = "ID de uma categoria sua (GET /api/categories). "
                + "É por aqui que se categoriza uma despesa criada sem categoria.",
                example = "e48a4177-d76c-4839-a034-237d3c41c175")
        @NotNull(message = "A categoria é obrigatória.")
        UUID categoryId,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "A data de vencimento é obrigatória.")
        LocalDate dueDate
) {
    public UpdateExpenseCommand toCommand(UUID expenseId, UUID userId) {
        return new UpdateExpenseCommand(expenseId, userId, categoryId, amount, currency,
                description, dueDate);
    }
}
