package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase.DepositCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record DepositRequest(

        @NotNull(message = "O usuário é obrigatório.")
        UUID userId,

        @NotNull(message = "O valor do aporte é obrigatório.")
        @DecimalMin(value = "0.01", message = "O aporte deve ser maior que zero.")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency
) {
    public DepositCommand toCommand(UUID goalId) {
        return new DepositCommand(goalId, userId, amount, currency);
    }
}
