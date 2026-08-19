package br.com.gastos.financeiro.infrastructure.database.repository;

import br.com.gastos.financeiro.infrastructure.database.entity.ExpenseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataExpenseRepository extends JpaRepository<ExpenseJpaEntity, UUID> {

    List<ExpenseJpaEntity> findByUserId(UUID userId);

    List<ExpenseJpaEntity> findByUserIdAndDueDateBetween(UUID userId, LocalDate startDate, LocalDate endDate);
}