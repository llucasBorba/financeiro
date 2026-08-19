package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.dto.CreateExpenseCommand;
import br.com.gastos.financeiro.core.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface CreateExpenseUseCase {

    Expense execute(CreateExpenseCommand command);
}
