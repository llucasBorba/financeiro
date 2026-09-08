package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.UpdateGoalUseCase.UpdateGoalCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Sem {@code currentAmount}: o saldo acumulado muda por aporte
 * ({@code POST /{id}/deposits}), nunca por edição.
 */
public record UpdateGoalRequest(

        @Schema(example = "Reserva de emergência")
        @NotBlank(message = "O título da meta é obrigatório.")
        @Size(max = 120, message = "O título deve ter no máximo 120 caracteres.")
        String title,

        @Schema(example = "12000.00")
        @NotNull(message = "O valor alvo é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor alvo deve ser maior que zero.")
        BigDecimal targetAmount,

        @Schema(description = "Precisa ser a mesma moeda do saldo já acumulado.",
                example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Schema(example = "2027-06-01")
        LocalDate targetDate
) {
    public UpdateGoalCommand toCommand(UUID goalId, UUID userId) {
        return new UpdateGoalCommand(goalId, userId, title, targetAmount, currency, targetDate);
    }
}
