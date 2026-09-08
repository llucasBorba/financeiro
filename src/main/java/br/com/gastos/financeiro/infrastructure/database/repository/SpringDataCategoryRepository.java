package br.com.gastos.financeiro.infrastructure.database.repository;

import br.com.gastos.financeiro.infrastructure.database.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SpringDataCategoryRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    List<CategoryJpaEntity> findByUserIdOrderByNameAsc(UUID userId);

    List<CategoryJpaEntity> findByUserIdAndActiveTrueOrderByNameAsc(UUID userId);

    /** O sufixo IgnoreCase faz o Spring Data gerar "WHERE upper(name) = upper(?)". */
    Optional<CategoryJpaEntity> findByUserIdAndNameIgnoreCase(UUID userId, String name);
}
