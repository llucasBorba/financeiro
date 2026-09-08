package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Estende o horizonte de um modelo, criando as ocorrências que faltam até o mês alvo.
 *
 * <p>É idempotente: mês que já tem ocorrência é pulado. Rodar duas vezes não duplica nada.
 */
public interface GenerateOccurrencesUseCase {

    record GenerateOccurrencesCommand(UUID recurringExpenseId, UUID userId, YearMonth through) {}

    /** @return apenas as ocorrências criadas agora. */
    List<Expense> execute(GenerateOccurrencesCommand command);
}
