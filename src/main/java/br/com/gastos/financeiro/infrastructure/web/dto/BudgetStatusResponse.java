package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.BudgetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * O limite de uma categoria confrontado com o mês consultado.
 *
 * <p>{@code spent} e {@code pending} vêm separados de propósito, e as duas porcentagens
 * existem por causa disso: {@code usedPercentage} mede o que já saiu, {@code projectedPercentage}
 * inclui o que ainda vai sair. É a segunda que avisa a tempo — a primeira só confirma o
 * estouro depois que ele aconteceu.
 */
public record BudgetStatusResponse(

        UUID categoryId,
        String categoryName,

        @Schema(description = "O limite definido para a categoria.", example = "1000.00")
        BigDecimal monthlyLimit,
        String currency,

        @Schema(description = "Despesas desta categoria PAGAS dentro do mês.", example = "500.00")
        BigDecimal spent,

        @Schema(description = "Despesas em aberto vencendo até o fim do mês, incluindo atrasadas.",
                example = "400.00")
        BigDecimal pending,

        @Schema(description = "Limite menos o gasto. Negativo quando estourou.", example = "500.00")
        BigDecimal remaining,

        @Schema(description = "Percentual do limite já gasto.", example = "50.00")
        BigDecimal usedPercentage,

        @Schema(description = "Percentual considerando gasto mais o que está a pagar.", example = "90.00")
        BigDecimal projectedPercentage,

        @Schema(description = "Já passou do limite com dinheiro que saiu.", example = "false")
        boolean exceeded,

        @Schema(description = "Vai passar do limite se tudo que está em aberto for pago.", example = "false")
        boolean projectedToExceed
) {
    public static BudgetStatusResponse from(BudgetStatus status) {
        return new BudgetStatusResponse(
                status.getCategoryId(),
                status.getCategoryName(),
                status.getMonthlyLimit().getAmount(),
                status.getMonthlyLimit().getCurrency(),
                status.getSpent().getAmount(),
                status.getPending().getAmount(),
                status.remaining(),
                status.usedPercentage(),
                status.projectedPercentage(),
                status.isExceeded(),
                status.isProjectedToExceed());
    }
}
