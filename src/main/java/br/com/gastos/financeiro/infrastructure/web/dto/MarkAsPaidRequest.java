package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase.MarkAsPaidCommand;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record MarkAsPaidRequest(

        @NotNull(message = "O usuário é obrigatório.")
        UUID userId,

        // Opcional: quando ausente, o domínio assume o momento atual.
        LocalDateTime paymentDate
) {
    public MarkAsPaidCommand toCommand(UUID expenseId) {
        return new MarkAsPaidCommand(expenseId, userId, paymentDate);
    }
}
