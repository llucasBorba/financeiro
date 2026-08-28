package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase.CreateGoalCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateGoalRequest(

        @NotNull(message = "O usuário é obrigatório.")
        UUID userId,

        @NotBlank(message = "O título da meta é obrigatório.")
        @Size(max = 120, message = "O título deve ter no máximo 120 caracteres.")
        String title,

        @NotNull(message = "O valor alvo é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor alvo deve ser maior que zero.")
        BigDecimal targetAmount,

        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        LocalDate targetDate
) {
    public CreateGoalCommand toCommand() {
        return new CreateGoalCommand(userId, title, targetAmount, currency, targetDate);
    }
}
