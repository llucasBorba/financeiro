package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.infrastructure.database.entity.FinancialGoalJpaEntity;

public class FinancialGoalMapper {

    private FinancialGoalMapper() {}

    /**
     * Copia o estado do domínio para uma entidade JPA — nova ou já carregada do banco.
     * Ver {@link ExpenseMapper#applyTo} para o motivo de receber a entidade de destino.
     */
    public static FinancialGoalJpaEntity applyTo(FinancialGoal domain, FinancialGoalJpaEntity entity) {
        if (domain == null) return null;

        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setTitle(domain.getTitle());
        entity.setTargetAmount(domain.getTargetAmount().getAmount());
        entity.setCurrentAmount(domain.getCurrentAmount().getAmount());
        entity.setCurrency(domain.getTargetAmount().getCurrency());
        entity.setTargetDate(domain.getTargetDate());

        return entity;
    }

    // Converte do JPA (Database) -> Core (Domain)
    public static FinancialGoal toDomain(FinancialGoalJpaEntity entity) {
        if (entity == null) return null;

        Money targetAmount = new Money(entity.getTargetAmount(), entity.getCurrency());
        Money currentAmount = new Money(entity.getCurrentAmount(), entity.getCurrency());

        return FinancialGoal.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getTitle(),
                targetAmount,
                currentAmount,
                entity.getTargetDate()
        );
    }
}
