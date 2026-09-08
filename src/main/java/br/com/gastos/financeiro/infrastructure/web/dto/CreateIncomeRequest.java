package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.CreateIncomeUseCase.CreateIncomeCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateIncomeRequest(

        @Schema(description = "Valor recebido, com no máximo 2 casas decimais.", example = "5000.00")
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @Schema(description = "Código ISO 4217. Omitido, assume BRL.", example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Schema(description = "Obrigatória: como receita não tem categoria, é ela que identifica "
                + "o lançamento. \"Salário setembro\", \"Pix do pai\", \"Freela projeto X\".",
                example = "Salário setembro")
        @NotBlank(message = "A descrição é obrigatória.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @Schema(description = "Quando o dinheiro entrou.", example = "2026-09-05")
        @NotNull(message = "A data de recebimento é obrigatória.")
        LocalDate receivedAt
) {
    public CreateIncomeCommand toCommand(UUID userId) {
        return new CreateIncomeCommand(userId, amount, currency, description, receivedAt);
    }
}
