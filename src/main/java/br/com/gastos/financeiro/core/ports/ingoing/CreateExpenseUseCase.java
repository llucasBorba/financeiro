package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public interface CreateExpenseUseCase {

    record CreateExpenseCommand(
            UUID userId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate dueDate,

            /**
             * Quando a despesa já foi paga no ato — a compra no mercado, o café, o
             * estacionamento. Preenchido, a despesa nasce PAID; ausente, nasce PENDING.
             *
             * <p>O ciclo "a vencer → pago" existe para contas que chegam antes do pagamento.
             * Obrigar uma compra à vista a passar por ele seria atrito sem propósito.
             */
            LocalDateTime paidAt
    ) {}

    Expense execute(CreateExpenseCommand command);
}
