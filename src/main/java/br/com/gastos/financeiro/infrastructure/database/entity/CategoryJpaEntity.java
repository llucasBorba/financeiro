package br.com.gastos.financeiro.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.util.UUID;

/**
 * Índices e constraints vivem apenas nas migrações Flyway — ver {@code V3__create_categories.sql}.
 * Declará-los aqui seria documentação capaz de mentir: com {@code ddl-auto=validate} o Hibernate
 * não confere índices nem constraints.
 */
@Entity
@Table(name = "tb_categories")
public class CategoryJpaEntity {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 60)
    private String name;

    @Column(nullable = false)
    private boolean active;

    @Version
    private Long version;

    public CategoryJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    /** Sem setter de propósito: quem controla a versão é o Hibernate. */
    public Long getVersion() { return version; }
}
