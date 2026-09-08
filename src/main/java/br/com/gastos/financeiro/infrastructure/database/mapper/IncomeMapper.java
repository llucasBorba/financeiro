package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.infrastructure.database.entity.IncomeJpaEntity;

public class IncomeMapper {

    private IncomeMapper() {}

    /** Ver {@link ExpenseMapper#applyTo} para o motivo de receber a entidade de destino. */
    public static IncomeJpaEntity applyTo(Income domain, IncomeJpaEntity entity) {
        if (domain == null) return null;

        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setAmount(domain.getAmount().getAmount());
        entity.setCurrency(domain.getAmount().getCurrency());
        entity.setDescription(domain.getDescription());
        entity.setReceivedAt(domain.getReceivedAt());

        return entity;
    }

    public static Income toDomain(IncomeJpaEntity entity) {
        if (entity == null) return null;

        return Income.reconstitute(
                entity.getId(),
                entity.getUserId(),
                new Money(entity.getAmount(), entity.getCurrency()),
                entity.getDescription(),
                entity.getReceivedAt());
    }
}
