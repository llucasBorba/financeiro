package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;
import br.com.gastos.financeiro.core.model.enums.ExpenseType;
import br.com.gastos.financeiro.infrastructure.database.entity.ExpenseJpaEntity;

public class ExpenseMapper {

    // Converte do Core (Domain) -> JPA (Database)
    public static ExpenseJpaEntity toJpaEntity(Expense domain) {
        if (domain == null) return null;

        return new ExpenseJpaEntity(
                domain.getId(),
                domain.getUserId(),
                domain.getCategoryId(),
                domain.getAmount().getAmount(),
                domain.getAmount().getCurrency(),
                domain.getDescription(),
                domain.getDueDate(),
                domain.getPaidAt(),
                domain.getType().name(),
                domain.getStatus().name()
        );
    }

    // Converte do JPA (Database) -> Core (Domain)
    public static Expense toDomain(ExpenseJpaEntity entity) {
        if (entity == null) return null;

        Money amount = new Money(entity.getAmount(), entity.getCurrency());
        ExpenseType type = ExpenseType.valueOf(entity.getType());

        Expense expense = new Expense(
                entity.getId(),
                entity.getUserId(),
                entity.getCategoryId(),
                amount,
                entity.getDescription(),
                entity.getDueDate(),
                type
        );

        // Se no banco o status já estiver marcado como PAID, atualiza o modelo de domínio
        if (ExpenseStatus.PAID.name().equals(entity.getStatus())) {
            expense.markAsPaid(entity.getPaidAt());
        }

        return expense;
    }
}