package br.com.gastos.financeiro.core.ports.ingoing;

import br.com.gastos.financeiro.core.model.Budget;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Define ou altera o limite mensal de uma categoria.
 *
 * <p>Uma operação só, e não um par criar/atualizar, porque o recurso é endereçado pela
 * categoria e existe no máximo um por categoria: "definir o limite de Alimentação em R$ 1.000"
 * tem o mesmo efeito havendo ou não um limite anterior. É o que permite o verbo PUT no
 * controller, com a idempotência que ele promete.
 */
public interface SetBudgetUseCase {

    record SetBudgetCommand(
            UUID userId,
            UUID categoryId,
            BigDecimal monthlyLimit,
            String currency
    ) {}

    Budget execute(SetBudgetCommand command);
}
