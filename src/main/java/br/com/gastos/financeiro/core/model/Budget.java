package br.com.gastos.financeiro.core.model;

import java.util.Objects;
import java.util.UUID;

/**
 * Limite mensal de gasto para uma categoria.
 *
 * <p>É um limite <em>fixo</em>: vale para todos os meses, não para um mês específico. Foi uma
 * decisão deliberada — orçamento doméstico muda de ano em ano, não de mês em mês, e obrigar o
 * usuário a redigitar "Alimentação: R$ 1.000" todo mês transformaria a funcionalidade em
 * trabalho. Se um dia fizer falta ajustar um mês pontual, o caminho é uma exceção por mês
 * sobrepondo este valor, não trocar o modelo.
 *
 * <p>Existe no máximo um orçamento por categoria, garantido por índice único no banco. Por
 * isso a API endereça o recurso pela categoria ({@code PUT /api/budgets/{categoryId}}) e a
 * gravação é idempotente: repetir a mesma requisição não cria um segundo orçamento.
 *
 * <p>Estourar o limite não bloqueia nada. Bloquear o registro de uma despesa real porque ela
 * passou do orçamento seria inverter o propósito: o app existe para refletir o que aconteceu,
 * e uma conta que chegou chegou. O limite serve para avisar, não para impedir.
 */
public class Budget {

    private final UUID id;
    private final UUID userId;
    private final UUID categoryId;
    private Money monthlyLimit;

    public Budget(UUID id, UUID userId, UUID categoryId, Money monthlyLimit) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.categoryId = Objects.requireNonNull(categoryId, "A categoria é obrigatória.");
        this.monthlyLimit = requirePositive(monthlyLimit);
    }

    public void changeLimit(Money novoLimite) {
        this.monthlyLimit = requirePositive(novoLimite);
    }

    private static Money requirePositive(Money limite) {
        Objects.requireNonNull(limite, "O limite mensal é obrigatório.");
        if (limite.isZero()) {
            // Limite zero seria "não posso gastar nada nesta categoria", que na prática é o
            // mesmo que não ter a categoria. Quem quer parar de usar arquiva a categoria.
            throw new IllegalArgumentException("O limite mensal deve ser maior que zero.");
        }
        return limite;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getCategoryId() { return categoryId; }
    public Money getMonthlyLimit() { return monthlyLimit; }
}
