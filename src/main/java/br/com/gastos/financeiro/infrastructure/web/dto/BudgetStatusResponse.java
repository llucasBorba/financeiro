package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.BudgetStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Uma categoria no mês consultado, com o limite dela quando existe.
 *
 * <p>Categoria que teve gasto mas não tem limite também aparece, com {@code hasBudget: false}
 * e todos os campos derivados do limite nulos. Nulo, e não zero: "0% usado" para quem gastou
 * R$ 800 seria um número que parece verdadeiro e não é.
 *
 * <p>{@code spent} e {@code pending} vêm separados de propósito, e as duas porcentagens
 * existem por causa disso: {@code usedPercentage} mede o que já saiu, {@code projectedPercentage}
 * inclui o que ainda vai sair. É a segunda que avisa a tempo — a primeira só confirma o
 * estouro depois que ele aconteceu.
 */
public record BudgetStatusResponse(

        UUID categoryId,
        String categoryName,

        @Schema(description = "Se existe limite definido. Quando falso, os campos que dependem "
                + "do limite vêm nulos.", example = "true")
        boolean hasBudget,

        @Schema(description = "O limite definido para a categoria. Nulo quando não há limite.",
                example = "1000.00", nullable = true)
        BigDecimal monthlyLimit,

        String currency,

        @Schema(description = "Despesas desta categoria PAGAS dentro do mês.", example = "500.00")
        BigDecimal spent,

        @Schema(description = "Despesas em aberto vencendo até o fim do mês, incluindo atrasadas.",
                example = "400.00")
        BigDecimal pending,

        @Schema(description = "Limite menos o gasto. Negativo quando estourou, nulo sem limite.",
                example = "500.00", nullable = true)
        BigDecimal remaining,

        @Schema(description = "Percentual do limite já gasto. Nulo sem limite.",
                example = "50.00", nullable = true)
        BigDecimal usedPercentage,

        @Schema(description = "Percentual considerando gasto mais o que está a pagar. Nulo sem limite.",
                example = "90.00", nullable = true)
        BigDecimal projectedPercentage,

        @Schema(description = "Já passou do limite com dinheiro que saiu. Falso sem limite.",
                example = "false")
        boolean exceeded,

        @Schema(description = "Vai passar do limite se tudo que está em aberto for pago.",
                example = "false")
        boolean projectedToExceed
) {
    public static BudgetStatusResponse from(BudgetStatus status) {
        return new BudgetStatusResponse(
                status.getCategoryId(),
                status.getCategoryName(),
                status.hasBudget(),
                status.hasBudget() ? status.getMonthlyLimit().getAmount() : null,
                status.getCurrency(),
                status.getSpent().getAmount(),
                status.getPending().getAmount(),
                status.remaining(),
                status.usedPercentage(),
                status.projectedPercentage(),
                status.isExceeded(),
                status.isProjectedToExceed());
    }
}
