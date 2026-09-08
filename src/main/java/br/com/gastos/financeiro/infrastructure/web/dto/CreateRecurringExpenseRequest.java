package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.CreateRecurringExpenseUseCase.CreateRecurringExpenseCommand;
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

public record CreateRecurringExpenseRequest(

        @Schema(description = "ID de uma categoria sua (GET /api/categories).",
                example = "e48a4177-d76c-4839-a034-237d3c41c175")
        @NotNull(message = "A categoria é obrigatória.")
        UUID categoryId,

        @Schema(description = "Valor de cada ocorrência.", example = "1500.00")
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @Schema(description = "Código ISO 4217. Omitido, assume BRL.", example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Schema(example = "Aluguel")
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @Schema(description = "Dia do vencimento. Em meses mais curtos, gruda no último dia "
                + "(31 vira 28 ou 29 em fevereiro).", example = "10")
        @NotNull(message = "O dia do vencimento é obrigatório.")
        @Min(value = 1, message = "O dia deve estar entre 1 e 31.")
        @Max(value = 31, message = "O dia deve estar entre 1 e 31.")
        Integer dayOfMonth,

        @Schema(description = "Primeiro mês da série, no formato yyyy-MM.", example = "2026-10")
        @NotNull(message = "O mês inicial é obrigatório.")
        YearMonth startMonth,

        @Schema(description = "Último mês da série. Omitido, a recorrência não tem fim previsto "
                + "e são geradas 12 ocorrências por vez.", example = "2027-09")
        YearMonth endMonth
) {
    public CreateRecurringExpenseCommand toCommand(UUID userId) {
        return new CreateRecurringExpenseCommand(userId, categoryId, amount, currency,
                description, dayOfMonth, startMonth, endMonth);
    }
}
