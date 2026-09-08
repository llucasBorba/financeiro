package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.MonthlySummary;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Schema(description = "Fechamento de um mês em uma moeda.")
public record MonthlySummaryResponse(

        @Schema(example = "2026-09") String month,
        @Schema(example = "BRL") String currency,

        @Schema(description = "Receitas recebidas no mês.", example = "5300.00")
        BigDecimal received,

        @Schema(description = "Despesas PAGAS no mês, independente de quando venceram.", example = "3200.00")
        BigDecimal paid,

        @Schema(description = "Tudo que vence até o fim do mês e segue pendente, inclusive o que "
                + "venceu antes. Informativo: NÃO entra no saldo, porque o dinheiro ainda não saiu.",
                example = "1050.00")
        BigDecimal pending,

        @Schema(description = "Parte do 'a pagar' que já passou do vencimento — a dívida "
                + "acumulada de meses anteriores.", example = "250.00")
        BigDecimal overdue,

        @Schema(description = "Entrou menos saiu. Pode ser negativo.", example = "2100.00")
        BigDecimal balance,

        @Schema(description = "Saldo se tudo que vence no mês for pago. Pode ser negativo.",
                example = "1300.00")
        BigDecimal projectedBalance,

        int receivedCount,
        int paidCount,
        int pendingCount,
        int overdueCount,

        @Schema(description = "Onde o dinheiro foi — quebra do que foi PAGO, maior primeiro.")
        List<CategoryTotalResponse> byCategory
) {

    public record CategoryTotalResponse(
            UUID categoryId,
            String categoryName,
            BigDecimal total,
            @Schema(description = "Fatia do total pago no mês.", example = "46.9") BigDecimal percentage
    ) {}

    public static MonthlySummaryResponse from(MonthlySummary summary) {
        return new MonthlySummaryResponse(
                summary.getMonth().toString(),
                summary.getCurrency(),
                summary.getReceived().getAmount(),
                summary.getPaid().getAmount(),
                summary.getPending().getAmount(),
                summary.getOverdue().getAmount(),
                summary.balance(),
                summary.projectedBalance(),
                summary.getReceivedCount(),
                summary.getPaidCount(),
                summary.getPendingCount(),
                summary.getOverdueCount(),
                summary.getByCategory().stream()
                        .map(c -> new CategoryTotalResponse(
                                c.categoryId(), c.categoryName(), c.total().getAmount(), c.percentage()))
                        .toList());
    }
}
