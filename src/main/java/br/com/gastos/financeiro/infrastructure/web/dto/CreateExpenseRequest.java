package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase.CreateExpenseCommand;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateExpenseRequest(

        // TODO: quando a autenticacao entrar, o userId sai do corpo e passa a vir do token.
        @NotNull(message = "O usuário é obrigatório.")
        UUID userId,

        UUID categoryId,

        @NotNull(message = "O valor é obrigatório.")
        @DecimalMin(value = "0.01", message = "O valor deve ser maior que zero.")
        BigDecimal amount,

        @Size(min = 3, max = 3, message = "A moeda deve ter 3 letras (ex.: BRL).")
        String currency,

        @Size(max = 255, message = "A descrição deve ter no máximo 255 caracteres.")
        String description,

        @NotNull(message = "A data de vencimento é obrigatória.")
        LocalDate dueDate,

        @NotBlank(message = "O tipo é obrigatório. Use 'FIXED' ou 'VARIABLE'.")
        String type
) {
    public CreateExpenseCommand toCommand() {
        return new CreateExpenseCommand(userId, categoryId, amount, currency, description, dueDate, type);
    }
}
