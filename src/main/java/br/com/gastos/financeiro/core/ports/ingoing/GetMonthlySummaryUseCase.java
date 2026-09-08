package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.MonthlySummary;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Devolve uma lista porque há um fechamento por moeda. Com uma moeda só — o caso normal —
 * a lista tem um item.
 */
public interface GetMonthlySummaryUseCase {

    List<MonthlySummary> execute(UUID userId, YearMonth month);
}
