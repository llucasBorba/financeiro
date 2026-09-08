package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.util.UUID;

/**
 * Desfaz um pagamento marcado por engano, devolvendo a despesa para "a pagar".
 *
 * <p>Usa um command record em vez de {@code execute(UUID, UUID)} porque
 * {@link DeleteExpenseUseCase} já ocupa essa assinatura — e Java não distingue
 * sobrecargas pelo tipo de retorno.
 */
public interface UndoExpensePaymentUseCase {

    record UndoPaymentCommand(UUID expenseId, UUID userId) {}

    Expense execute(UndoPaymentCommand command);
}
