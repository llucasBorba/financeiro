package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.ExpenseJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.ExpenseMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataExpenseRepository;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ExpenseRepositoryAdapter implements ExpenseRepositoryPort {

    private final SpringDataExpenseRepository jpaRepository;

    public ExpenseRepositoryAdapter(SpringDataExpenseRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /**
     * Grava a despesa reaproveitando a linha existente, quando houver.
     *
     * <p>O {@code findById} carrega a entidade gerenciada (e com ela a versão lida do banco)
     * para que o Hibernate consiga checar a concorrência no UPDATE. Dentro de uma transação
     * essa busca costuma ser acerto no cache de primeiro nível, sem ida extra ao banco —
     * e mesmo quando vai, é o mesmo SELECT que o {@code merge()} já fazia por baixo dos panos.
     */
    @Override
    public Expense save(Expense expense) {
        ExpenseJpaEntity entity = jpaRepository.findById(expense.getId())
                .orElseGet(ExpenseJpaEntity::new);

        ExpenseJpaEntity saved = jpaRepository.save(ExpenseMapper.applyTo(expense, entity));
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
    public List<Expense> findByUserIdAndPaidAtBetween(UUID userId, LocalDateTime from, LocalDateTime to) {
        return jpaRepository.findByUserIdAndPaidAtBetween(userId, from, to).stream()
                .map(ExpenseMapper::toDomain)
                .toList();
    }

    @Override
    public List<Expense> findByUserIdAndDueDateUpTo(UUID userId, LocalDate limit) {
        return jpaRepository.findByUserIdAndDueDateLessThanEqual(userId, limit).stream()
                .map(ExpenseMapper::toDomain)
                .toList();
    }

    @Override
    public List<Expense> findByRecurringExpenseId(UUID recurringExpenseId) {
        return jpaRepository.findByRecurringExpenseId(recurringExpenseId).stream()
                .map(ExpenseMapper::toDomain)
                .toList();
    }

    @Override
    public long countByCategoryId(UUID categoryId) {
        return jpaRepository.countByCategoryId(categoryId);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }
}
