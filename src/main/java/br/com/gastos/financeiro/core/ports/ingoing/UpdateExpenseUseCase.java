package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Expense;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Substituição completa dos dados editáveis de uma despesa (semântica de PUT).
 *
 * <p>Campo ausente vira nulo — não é atualização parcial. Se um dia fizer sentido alterar
 * só a descrição, isso vira outro caso de uso (PATCH), com semântica própria: misturar
 * "substituir tudo" e "alterar um pedaço" no mesmo comando gera ambiguidade sobre o que
 * um nulo significa (apagar o campo? ou não mexer nele?).
 */
public interface UpdateExpenseUseCase {

    record UpdateExpenseCommand(
            UUID expenseId,
            UUID userId,
            UUID categoryId,
            BigDecimal amount,
            String currency,
            String description,
            LocalDate dueDate
    ) {}

    Expense execute(UpdateExpenseCommand command);
}
