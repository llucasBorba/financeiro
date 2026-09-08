package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FindExpenseUseCase {

    Expense findById(UUID expenseId, UUID userId);

    List<Expense> listByUser(UUID userId);

    List<Expense> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate);

    /**
     * Ocorrências geradas por uma recorrência — responde "o que já gerei dessa regra?".
     *
     * <p>{@code startDate} e {@code endDate} são opcionais e compõem o filtro. Recorrência de
     * outro usuário devolve lista vazia, não erro: nem confirma nem nega que aquele id existe.
     */
    List<Expense> listByUserAndRecurrence(UUID userId, UUID recurringExpenseId,
                                          LocalDate startDate, LocalDate endDate);
}
