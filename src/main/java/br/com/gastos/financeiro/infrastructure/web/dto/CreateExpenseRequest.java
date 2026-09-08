package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase.CreateExpenseCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * O {@code userId} não está aqui de propósito: identidade não é algo que o cliente informa,
 * é algo que o servidor descobre. Ele chega ao {@code toCommand} vindo do
 * {@code CurrentUserProvider}.
 */
public record CreateExpenseRequest(

        @Schema(description = "ID de uma categoria sua. Descubra os seus em GET /api/categories.",
                example = "e48a4177-d76c-4839-a034-237d3c41c175")
        @NotNull(message = "A categoria é obrigatória.")
        UUID categoryId,

        @Schema(description = "Valor da despesa, com no máximo 2 casas decimais.", example = "19.01")
        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @Schema(description = "Código ISO 4217. Omitido, assume BRL.", example = "BRL", defaultValue = "BRL")
        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Schema(description = "Texto livre para você reconhecer o lançamento.")
        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @Schema(description = "Data a que a despesa se refere: o vencimento de uma conta "
                + "ou a data da compra, no caso de gasto à vista.", example = "2026-09-08")
        @NotNull(message = "A data de vencimento é obrigatória.")
        LocalDate dueDate,


        /**
         * Opcional. Preenchido, a despesa já nasce paga — o caso da compra no mercado, que
         * não tem ciclo "a vencer". Ausente, nasce pendente e é quitada depois via
         * {@code PATCH /api/expenses/{id}/payment}.
         */
        @Schema(description = "Preenchido, a despesa já nasce PAGA — o caso da compra à vista. "
                + "Ausente, nasce PENDENTE e é quitada depois em PATCH /api/expenses/{id}/payment.",
                example = "2026-09-08T12:30:00")
        LocalDateTime paidAt
) {
    public CreateExpenseCommand toCommand(UUID userId) {
        return new CreateExpenseCommand(userId, categoryId, amount, currency, description,
                dueDate, paidAt);
    }
}
