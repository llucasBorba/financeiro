package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.ExpenseJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.ExpenseMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataExpenseRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ExpenseRepositoryAdapter implements ExpenseRepositoryPort {

    private final SpringDataExpenseRepository jpaRepository;

    public ExpenseRepositoryAdapter(SpringDataExpenseRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Expense save(Expense expense) {
        ExpenseJpaEntity saved = jpaRepository.save(ExpenseMapper.toJpaEntity(expense));
        return ExpenseMapper.toDomain(saved);
    }

    @Override
    public Optional<Expense> findById(UUID id) {
        return jpaRepository.findById(id).map(ExpenseMapper::toDomain);
    }

    @Override
    public List<Expense> findByUserId(UUID userId) {
        return jpaRepository.findByUserId(userId).stream()
                .map(ExpenseMapper::toDomain)
                .toList();
    }

    @Override
    public List<Expense> findByUserIdAndDueDateBetween(UUID userId, LocalDate startDate, LocalDate endDate) {
        return jpaRepository.findByUserIdAndDueDateBetween(userId, startDate, endDate).stream()
                .map(ExpenseMapper::toDomain)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
