package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.SetBudgetUseCase.SetBudgetCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record SetBudgetRequest(

        @Schema(description = "Quanto se pretende gastar por mês nesta categoria.", example = "1000.00")
        @NotNull(message = "O limite mensal é obrigatório.")
        @DecimalMin(value = "0.01", message = "O limite mensal deve ser maior que zero.")
        @Digits(integer = 13, fraction = 2, message = "O limite mensal excede o valor máximo permitido.")
        BigDecimal monthlyLimit,

        @Schema(description = "Moeda do limite. Despesas em outra moeda não entram nesta conta.",
                example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency
) {
    public SetBudgetCommand toCommand(UUID userId, UUID categoryId) {
        return new SetBudgetCommand(userId, categoryId, monthlyLimit, currency);
    }
}
