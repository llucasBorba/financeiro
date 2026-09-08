package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;
import br.com.gastos.financeiro.infrastructure.database.entity.ExpenseJpaEntity;

public class ExpenseMapper {

    private ExpenseMapper() {}

    /**
     * Copia o estado do domínio para uma entidade JPA — nova ou já carregada do banco.
     *
     * <p>Recebe a entidade de destino em vez de criar uma do zero porque, num UPDATE, a
     * instância carregada carrega a versão lida do banco. Se montássemos uma entidade nova
     * a cada gravação, o campo {@code version} viria nulo e o controle de concorrência
     * otimista se perderia (o Spring Data ainda acharia que a linha é nova).
     *
     * <p>Repare que {@code version} nunca é copiado daqui: quem escreve nele é o Hibernate.
     */
    public static ExpenseJpaEntity applyTo(Expense domain, ExpenseJpaEntity entity) {
        if (domain == null) return null;

        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setCategoryId(domain.getCategoryId());
        entity.setAmount(domain.getAmount().getAmount());
        entity.setCurrency(domain.getAmount().getCurrency());
        entity.setDescription(domain.getDescription());
        entity.setDueDate(domain.getDueDate());
        entity.setPaidAt(domain.getPaidAt());
        entity.setStatus(domain.getStatus().name());
        entity.setRecurringExpenseId(domain.getRecurringExpenseId());

        return entity;
    }

    // Converte do JPA (Database) -> Core (Domain)
    public static Expense toDomain(ExpenseJpaEntity entity) {
        if (entity == null) return null;

        Money amount = new Money(entity.getAmount(), entity.getCurrency());

        return Expense.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getCategoryId(),
                amount,
                entity.getDescription(),
                entity.getDueDate(),
                ExpenseStatus.valueOf(entity.getStatus()),
                entity.getPaidAt(),
                entity.getRecurringExpenseId()
        );
    }
}
