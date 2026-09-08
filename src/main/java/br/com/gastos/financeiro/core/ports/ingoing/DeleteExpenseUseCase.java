package br.com.gastos.financeiro.core.ports.ingoing;

import java.util.UUID;

public interface DeleteExpenseUseCase {

    /**
     * Remove a despesa. Recebe o {@code userId} porque a posse é verificada aqui dentro:
     * um usuário não pode apagar a despesa de outro nem descobrir que ela existe.
     */
    void execute(UUID expenseId, UUID userId);
}
