package br.com.gastos.financeiro.core.ports.ingoing;

import java.util.UUID;

/**
 * Apaga a meta. Diferente de categoria, nada aponta para uma meta — o saldo acumulado é um
 * número dentro dela, não lançamentos externos. Por isso não há exclusão bloqueada aqui.
 */
public interface DeleteGoalUseCase {

    void execute(UUID goalId, UUID userId);
}
