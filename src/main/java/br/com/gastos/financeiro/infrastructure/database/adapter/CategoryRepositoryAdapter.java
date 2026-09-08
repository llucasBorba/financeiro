package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.CategoryJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.CategoryMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataCategoryRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class CategoryRepositoryAdapter implements CategoryRepositoryPort {

    private final SpringDataCategoryRepository jpaRepository;

    public CategoryRepositoryAdapter(SpringDataCategoryRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Ver {@code ExpenseRepositoryAdapter#save} para o motivo de carregar antes de gravar. */
    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = jpaRepository.findById(category.getId())
                .orElseGet(CategoryJpaEntity::new);

        CategoryJpaEntity saved = jpaRepository.save(CategoryMapper.applyTo(category, entity));
        return CategoryMapper.toDomain(saved);
    }

    /**
     * Grava várias de uma vez. Usado só ao semear conta nova: são 8 linhas que nunca existiram,
     * então não há o que recarregar antes — daí não passar pelo {@link #save}.
     */
    @Override
    public List<Category> saveAll(List<Category> categories) {
        List<CategoryJpaEntity> entities = categories.stream()
                .map(c -> CategoryMapper.applyTo(c, new CategoryJpaEntity()))
                .toList();

        return jpaRepository.saveAll(entities).stream()
                .map(CategoryMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaRepository.findById(id).map(CategoryMapper::toDomain);
    }

    @Override
    public Optional<Category> findByUserIdAndNameIgnoreCase(UUID userId, String name) {
        return jpaRepository.findByUserIdAndNameIgnoreCase(userId, name).map(CategoryMapper::toDomain);
    }

    @Override
    public List<Category> findByUserId(UUID userId, boolean includeInactive) {
        List<CategoryJpaEntity> found = includeInactive
                ? jpaRepository.findByUserIdOrderByNameAsc(userId)
                : jpaRepository.findByUserIdAndActiveTrueOrderByNameAsc(userId);

        return found.stream().map(CategoryMapper::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
