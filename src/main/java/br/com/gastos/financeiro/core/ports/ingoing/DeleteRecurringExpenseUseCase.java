package br.com.gastos.financeiro.core.ports.ingoing;

import java.util.UUID;

/**
 * Apaga o modelo e as ocorrências futuras não pagas.
 *
 * <p>O histórico pago sobrevive: você pagou aquelas contas, elas aconteceram. Elas apenas
 * perdem o vínculo com o modelo e viram lançamentos avulsos — que é o que passam a ser.
 */
public interface DeleteRecurringExpenseUseCase {

    void execute(UUID recurringExpenseId, UUID userId);
}
