package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.ports.outgoing.FinancialGoalRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.FinancialGoalJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.FinancialGoalMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataFinancialGoalRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class FinancialGoalRepositoryAdapter implements FinancialGoalRepositoryPort {

    private final SpringDataFinancialGoalRepository jpaRepository;

    public FinancialGoalRepositoryAdapter(SpringDataFinancialGoalRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Ver {@code ExpenseRepositoryAdapter#save} para o motivo de carregar antes de gravar. */
    @Override
    public FinancialGoal save(FinancialGoal goal) {
        FinancialGoalJpaEntity entity = jpaRepository.findById(goal.getId())
                .orElseGet(FinancialGoalJpaEntity::new);

        FinancialGoalJpaEntity saved = jpaRepository.save(FinancialGoalMapper.applyTo(goal, entity));
        return FinancialGoalMapper.toDomain(saved);
    }

    @Override
    public Optional<FinancialGoal> findById(UUID id) {
        return jpaRepository.findById(id).map(FinancialGoalMapper::toDomain);
    }

    @Override
    public List<FinancialGoal> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(FinancialGoalMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
