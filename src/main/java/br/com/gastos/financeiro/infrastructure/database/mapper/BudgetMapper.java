package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.Budget;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.infrastructure.database.entity.BudgetJpaEntity;

public class BudgetMapper {

    private BudgetMapper() {}

    /** Ver {@link ExpenseMapper#applyTo} para o motivo de receber a entidade de destino. */
    public static BudgetJpaEntity applyTo(Budget domain, BudgetJpaEntity entity) {
        if (domain == null) return null;

        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setMonthlyLimit(domain.getMonthlyLimit().getAmount());
        entity.setCurrency(domain.getMonthlyLimit().getCurrency());

        return entity;
    }

    public static Budget toDomain(BudgetJpaEntity entity) {
        if (entity == null) return null;

        return new Budget(
                entity.getId(),
                entity.getUserId(),
                entity.getCategoryId(),
                new Money(entity.getMonthlyLimit(), entity.getCurrency()));
    }
}
