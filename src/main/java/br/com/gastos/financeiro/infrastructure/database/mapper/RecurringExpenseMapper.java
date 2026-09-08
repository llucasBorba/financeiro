package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.model.RecurringExpense;
import br.com.gastos.financeiro.infrastructure.database.entity.RecurringExpenseJpaEntity;

import java.time.LocalDate;
import java.time.YearMonth;

public class RecurringExpenseMapper {

    private RecurringExpenseMapper() {}

    /** Ver {@link ExpenseMapper#applyTo} para o motivo de receber a entidade de destino. */
    public static RecurringExpenseJpaEntity applyTo(RecurringExpense domain, RecurringExpenseJpaEntity entity) {
        if (domain == null) return null;

        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setAmount(domain.getAmount().getAmount());
        entity.setCurrency(domain.getAmount().getCurrency());
        entity.setDescription(domain.getDescription());
        entity.setDayOfMonth(domain.getDayOfMonth());
        entity.setStartMonth(toDate(domain.getStartMonth()));
        entity.setEndMonth(toDate(domain.getEndMonth()));
        entity.setActive(domain.isActive());

        return entity;
    }

    public static RecurringExpense toDomain(RecurringExpenseJpaEntity entity) {
        if (entity == null) return null;

        return RecurringExpense.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getCategoryId(),
                new Money(entity.getAmount(), entity.getCurrency()),
                entity.getDescription(),
                entity.getDayOfMonth(),
                toYearMonth(entity.getStartMonth()),
                toYearMonth(entity.getEndMonth()),
                entity.isActive());
    }

    /** Mês vira o dia 1º; o dia real do vencimento é calculado pelo domínio. */
    private static LocalDate toDate(YearMonth month) {
        return month != null ? month.atDay(1) : null;
    }

    private static YearMonth toYearMonth(LocalDate date) {
        return date != null ? YearMonth.from(date) : null;
    }
}
