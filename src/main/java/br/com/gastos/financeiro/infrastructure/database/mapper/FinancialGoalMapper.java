package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.infrastructure.database.entity.FinancialGoalJpaEntity;

public class FinancialGoalMapper {

    private FinancialGoalMapper() {}

    // Converte do Core (Domain) -> JPA (Database)
    public static FinancialGoalJpaEntity toJpaEntity(FinancialGoal domain) {
        if (domain == null) return null;

        return new FinancialGoalJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getTitle(),
                domain.getTargetAmount().getAmount(),
                domain.getCurrentAmount().getAmount(),
                domain.getTargetAmount().getCurrency(),
                domain.getTargetDate()
        );
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
