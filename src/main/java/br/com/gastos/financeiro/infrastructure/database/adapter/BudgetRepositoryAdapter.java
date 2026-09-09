package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.Budget;
import br.com.gastos.financeiro.core.ports.outgoing.BudgetRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.BudgetJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.BudgetMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataBudgetRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class BudgetRepositoryAdapter implements BudgetRepositoryPort {

    private final SpringDataBudgetRepository jpaRepository;

    public BudgetRepositoryAdapter(SpringDataBudgetRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Ver {@code ExpenseRepositoryAdapter#save} para o motivo do findById antes de gravar. */
    @Override
    public Budget save(Budget budget) {
        BudgetJpaEntity entity = jpaRepository.findById(budget.getId())
                .orElseGet(BudgetJpaEntity::new);

        return BudgetMapper.toDomain(jpaRepository.save(BudgetMapper.applyTo(budget, entity)));
    }

    @Override
    public List<Budget> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream().map(BudgetMapper::toDomain).toList();
    }

    @Override
    public Optional<Budget> findByUserIdAndCategoryId(UUID userId, UUID categoryId) {
        return jpaRepository.findByUserIdAndCategoryId(userId, categoryId).map(BudgetMapper::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
