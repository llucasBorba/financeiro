package br.com.gastos.financeiro.infrastructure.database.repository;

import br.com.gastos.financeiro.infrastructure.database.entity.IncomeJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface SpringDataIncomeRepository extends JpaRepository<IncomeJpaEntity, UUID> {

    List<IncomeJpaEntity> findByUserIdOrderByReceivedAtDesc(UUID userId);

    List<IncomeJpaEntity> findByUserIdAndReceivedAtBetweenOrderByReceivedAtDesc(
            UUID userId, LocalDate startDate, LocalDate endDate);
}
