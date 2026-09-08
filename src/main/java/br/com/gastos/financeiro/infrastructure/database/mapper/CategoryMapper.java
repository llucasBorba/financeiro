package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.infrastructure.database.entity.CategoryJpaEntity;

public class CategoryMapper {

    private CategoryMapper() {}

    /** Ver {@link ExpenseMapper#applyTo} para o motivo de receber a entidade de destino. */
    public static CategoryJpaEntity applyTo(Category domain, CategoryJpaEntity entity) {
        if (domain == null) return null;

        entity.setId(domain.getId());
        entity.setUserId(domain.getUserId());
        entity.setName(domain.getName());
        entity.setActive(domain.isActive());

        return entity;
    }

    public static Category toDomain(CategoryJpaEntity entity) {
        if (entity == null) return null;

        return Category.reconstitute(
                entity.getId(),
                entity.getUserId(),
                entity.getName(),
                entity.isActive());
    }
}
