package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.UpdateIncomeUseCase.UpdateIncomeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record UpdateIncomeRequest(

        @Schema(example = "5200.00")
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @Schema(example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Schema(example = "Salário setembro (com bônus)")
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @Schema(example = "2026-09-05")
        @NotNull(message = "A data de recebimento é obrigatória.")
        LocalDate receivedAt
) {
    public UpdateIncomeCommand toCommand(UUID incomeId, UUID userId) {
        return new UpdateIncomeCommand(incomeId, userId, amount, currency, description, receivedAt);
    }
}
