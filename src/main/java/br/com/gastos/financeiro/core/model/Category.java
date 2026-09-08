package br.com.gastos.financeiro.core.model;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Categoria de despesa — "Alimentação", "Moradia", "Transporte".
 *
 * <p>Serve para <strong>agregar</strong>: com 60 despesas por mês, ninguém lê a lista para
 * saber onde o dinheiro foi. Receitas não têm categoria de propósito — com 2 ou 3 lançamentos
 * mensais, a própria lista já é o relatório e classificar seria atrito sem retorno.
 *
 * <p>Categoria não se apaga quando está em uso; se arquiva. Excluir uma categoria com 200
 * despesas dentro destruiria a classificação de dois anos de histórico.
 */
public class Category {

    public static final int MAX_NAME_LENGTH = 60;

    /**
     * Ponto de partida de toda conta nova. São cópias do usuário: ele pode renomear,
     * arquivar ou apagar qualquer uma. Categoria padrão que não se pode ajustar é imposição,
     * não conveniência.
     */
    private static final List<String> DEFAULT_NAMES = List.of(
            "Alimentação", "Moradia", "Transporte", "Saúde",
            "Educação", "Lazer", "Compras", "Outros");

    private final UUID id;
    private final UUID userId;
    private String name;
    private boolean active;

    private Category(UUID id, UUID userId, String name, boolean active) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.name = normalizeName(name);
        this.active = active;
    }

    public static Category create(UUID userId, String name) {
        return new Category(null, userId, name, true);
    }

    /** Reconstituição a partir da persistência. */
    public static Category reconstitute(UUID id, UUID userId, String name, boolean active) {
        return new Category(id, userId, name, active);
    }

    /** As categorias com que uma conta recém-criada começa. */
    public static List<Category> defaultsFor(UUID userId) {
        return DEFAULT_NAMES.stream().map(name -> create(userId, name)).toList();
    }

    /**
     * Renomear é sempre seguro: as despesas apontam para o {@code id}, não para o texto.
     * Renomear "Alimentação" para "Comida" faz as 200 despesas exibirem o nome novo
     * sem nenhum UPDATE em cascata.
     */
    public void rename(String newName) {
        this.name = normalizeName(newName);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    private static String normalizeName(String name) {
        Objects.requireNonNull(name, "O nome da categoria é obrigatório.");
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("O nome da categoria é obrigatório.");
        }
        if (trimmed.length() > MAX_NAME_LENGTH) {
            throw new IllegalArgumentException(
                    "O nome da categoria deve ter no máximo " + MAX_NAME_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
}
