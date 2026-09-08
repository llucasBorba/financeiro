package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/** Substituição completa dos dados (semântica de PUT), como em UpdateExpenseUseCase. */
public interface UpdateIncomeUseCase {

    record UpdateIncomeCommand(
            UUID incomeId,
            UUID userId,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate receivedAt
    ) {}

    Income execute(UpdateIncomeCommand command);
}
