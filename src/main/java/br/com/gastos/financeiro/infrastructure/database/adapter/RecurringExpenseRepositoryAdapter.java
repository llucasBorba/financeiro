package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.RecurringExpense;
import br.com.gastos.financeiro.core.ports.outgoing.RecurringExpenseRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.RecurringExpenseJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.RecurringExpenseMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataRecurringExpenseRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class RecurringExpenseRepositoryAdapter implements RecurringExpenseRepositoryPort {

    private final SpringDataRecurringExpenseRepository jpaRepository;

    public RecurringExpenseRepositoryAdapter(SpringDataRecurringExpenseRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Ver {@code ExpenseRepositoryAdapter#save} para o motivo de carregar antes de gravar. */
    @Override
    public RecurringExpense save(RecurringExpense recurringExpense) {
        RecurringExpenseJpaEntity entity = jpaRepository.findById(recurringExpense.getId())
                .orElseGet(RecurringExpenseJpaEntity::new);

        RecurringExpenseJpaEntity saved =
                jpaRepository.save(RecurringExpenseMapper.applyTo(recurringExpense, entity));
        return RecurringExpenseMapper.toDomain(saved);
    }

    @Override
    public Optional<RecurringExpense> findById(UUID id) {
        return jpaRepository.findById(id).map(RecurringExpenseMapper::toDomain);
    }

    @Override
    public List<RecurringExpense> findByUserId(UUID userId) {
        return jpaRepository.findByUserIdOrderByDescriptionAsc(userId).stream()
                .map(RecurringExpenseMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
