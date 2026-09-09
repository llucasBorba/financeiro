package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.WithdrawFromGoalUseCase.WithdrawCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record WithdrawalRequest(

        @Schema(description = "Quanto está sendo resgatado agora. Não pode passar do saldo da meta.",
                example = "150.00")
        @NotNull(message = "O valor do resgate é obrigatório.")
        @DecimalMin(value = "0.01", message = "O resgate deve ser maior que zero.")
        BigDecimal amount,

        @Schema(description = "Precisa ser a mesma moeda da meta.", example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency
) {
    public WithdrawCommand toCommand(UUID goalId, UUID userId) {
        return new WithdrawCommand(goalId, userId, amount, currency);
    }
}
