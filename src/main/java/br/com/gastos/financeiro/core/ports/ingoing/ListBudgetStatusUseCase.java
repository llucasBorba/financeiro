package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.BudgetStatus;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/** Os limites do usuário confrontados com o que aconteceu num mês. */
public interface ListBudgetStatusUseCase {

    List<BudgetStatus> execute(UUID userId, YearMonth month);
}
