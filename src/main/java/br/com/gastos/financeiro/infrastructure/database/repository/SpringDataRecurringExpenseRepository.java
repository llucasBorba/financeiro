package br.com.gastos.financeiro.infrastructure.database.repository;

import br.com.gastos.financeiro.infrastructure.database.entity.RecurringExpenseJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataRecurringExpenseRepository extends JpaRepository<RecurringExpenseJpaEntity, UUID> {

    List<RecurringExpenseJpaEntity> findByUserIdOrderByDescriptionAsc(UUID userId);
}
