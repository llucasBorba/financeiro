package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Income;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateIncomeUseCase {

    record CreateIncomeCommand(
            UUID userId,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate receivedAt
    ) {}

    Income execute(CreateIncomeCommand command);
}
