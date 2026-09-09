package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.FinancialGoal;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Retira dinheiro de uma meta. Contrapartida de {@link DepositToGoalUseCase}.
 *
 * <p>É uma porta separada, e não um método a mais na porta de aporte, pelo mesmo motivo que
 * pagar e despagar uma despesa são operações distintas: quem consome a API declara qual
 * capacidade precisa, e um cliente que só deposita não ganha o poder de sacar por tabela.
 */
public interface WithdrawFromGoalUseCase {

    record WithdrawCommand(
            UUID goalId,
            UUID userId,
            BigDecimal amount,
            String currency
    ) {}

    FinancialGoal execute(WithdrawCommand command);
}
