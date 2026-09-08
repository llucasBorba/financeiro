package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adapter de teste para {@link CategoryRepositoryPort}: um HashMap no lugar do Postgres. */
public class InMemoryCategoryRepository implements CategoryRepositoryPort {

    private final Map<UUID, Category> storage = new LinkedHashMap<>();

    @Override
    public Category save(Category category) {
        storage.put(category.getId(), category);
        return category;
    }

    @Override
    public List<Category> saveAll(List<Category> categories) {
        categories.forEach(this::save);
        return categories;
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<Category> findByUserIdAndNameIgnoreCase(UUID userId, String name) {
        return storage.values().stream()
                .filter(c -> c.getUserId().equals(userId))
                .filter(c -> c.getName().equalsIgnoreCase(name))
                .findFirst();
    }

    @Override
    public List<Category> findByUserId(UUID userId, boolean includeInactive) {
        return storage.values().stream()
                .filter(c -> c.getUserId().equals(userId))
                .filter(c -> includeInactive || c.isActive())
                .sorted(Comparator.comparing(Category::getName))
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        storage.remove(id);
    }
}
