package br.com.gastos.financeiro.core.ports.ingoing;

import java.util.UUID;

public interface DeleteIncomeUseCase {

    /** Recebe o {@code userId} porque a posse é verificada aqui dentro. */
    void execute(UUID incomeId, UUID userId);
}
