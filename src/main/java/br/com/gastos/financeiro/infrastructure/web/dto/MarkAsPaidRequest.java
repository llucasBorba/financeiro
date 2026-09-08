package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase.MarkAsPaidCommand;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.util.UUID;

public record MarkAsPaidRequest(

        // Opcional: quando ausente, o domínio assume o momento atual.
        @Schema(description = "Quando foi pago. Omitido, assume o momento atual.",
                example = "2026-09-08T12:30:00")
        LocalDateTime paymentDate
) {
    public MarkAsPaidCommand toCommand(UUID expenseId, UUID userId) {
        return new MarkAsPaidCommand(expenseId, userId, paymentDate);
    }
}
