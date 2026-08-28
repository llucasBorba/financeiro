package br.com.gastos.financeiro.infrastructure.database.repository;

import br.com.gastos.financeiro.infrastructure.database.entity.FinancialGoalJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataFinancialGoalRepository extends JpaRepository<FinancialGoalJpaEntity, UUID> {

    List<FinancialGoalJpaEntity> findByUserId(UUID userId);
}
