package br.com.gastos.financeiro.core.ports.outgoing;

import br.com.gastos.financeiro.core.model.Category;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepositoryPort {

    Category save(Category category);

    /** Usado ao semear as categorias padrão de uma conta nova, numa tacada só. */
    List<Category> saveAll(List<Category> categories);

    Optional<Category> findById(UUID id);

    /**
     * Busca sem diferenciar maiúsculas — é assim que impedimos "Alimentação" e "alimentação"
     * de virarem duas categorias. O UNIQUE do banco é sensível a caixa, então esta checagem
     * no domínio é que garante a regra de verdade.
     */
    Optional<Category> findByUserIdAndNameIgnoreCase(UUID userId, String name);

    List<Category> findByUserId(UUID userId, boolean includeInactive);

    void deleteById(UUID id);
}
