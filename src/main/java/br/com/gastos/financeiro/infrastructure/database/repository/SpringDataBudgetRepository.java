package br.com.gastos.financeiro.infrastructure.database.repository;

import br.com.gastos.financeiro.infrastructure.database.entity.BudgetJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SpringDataBudgetRepository extends JpaRepository<BudgetJpaEntity, UUID> {

    List<BudgetJpaEntity> findByUserId(UUID userId);

    Optional<BudgetJpaEntity> findByUserIdAndCategoryId(UUID userId, UUID categoryId);
}
