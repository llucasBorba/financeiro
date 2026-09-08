package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.UpdateRecurringExpenseUseCase.UpdateRecurringExpenseCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

/**
 * O mês inicial não está aqui: ele define a origem da série e não muda. As alterações valem
 * para as ocorrências futuras e não pagas.
 */
public record UpdateRecurringExpenseRequest(

        @NotNull(message = "A categoria é obrigatória.")
        UUID categoryId,

        @Schema(example = "1600.00")
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @Schema(example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Schema(example = "Aluguel (reajustado)")
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @Schema(example = "10")
        @NotNull(message = "O dia do vencimento é obrigatório.")
        @Min(value = 1, message = "O dia deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia deve estar entre 1 e 31.")
        Integer dayOfMonth,

        @Schema(description = "Último mês da série. Nulo = sem fim previsto.", example = "2027-09")
        YearMonth endMonth
) {
    public UpdateRecurringExpenseCommand toCommand(UUID recurringExpenseId, UUID userId) {
        return new UpdateRecurringExpenseCommand(recurringExpenseId, userId, categoryId, amount,
                currency, description, dayOfMonth, endMonth);
    }
}
