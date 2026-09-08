package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Income;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface FindIncomeUseCase {

    Income findById(UUID incomeId, UUID userId);

    List<Income> listByUser(UUID userId);

    List<Income> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate);
}
