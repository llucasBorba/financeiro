package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.ports.outgoing.IncomeRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.IncomeJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.IncomeMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataIncomeRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class IncomeRepositoryAdapter implements IncomeRepositoryPort {

    private final SpringDataIncomeRepository jpaRepository;

    public IncomeRepositoryAdapter(SpringDataIncomeRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Ver {@code ExpenseRepositoryAdapter#save} para o motivo de carregar antes de gravar. */
    @Override
    public Income save(Income income) {
        IncomeJpaEntity entity = jpaRepository.findById(income.getId())
                .orElseGet(IncomeJpaEntity::new);

        IncomeJpaEntity saved = jpaRepository.save(IncomeMapper.applyTo(income, entity));
        return IncomeMapper.toDomain(saved);
    }

    @Override
    public Optional<Income> findById(UUID id) {
        return jpaRepository.findById(id).map(IncomeMapper::toDomain);
    }

    @Override
    public List<Income> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByReceivedAtDesc(userId).stream()
                .map(IncomeMapper::toDomain)
                .toList();
    }

    @Override
    public List<Income> findByUserIdAndReceivedAtBetween(UUID userId, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByUserIdAndReceivedAtBetweenOrderByReceivedAtDesc(userId, startDate, endDate)
                .stream()
                .map(IncomeMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
