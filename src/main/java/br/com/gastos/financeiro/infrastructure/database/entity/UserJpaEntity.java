package br.com.gastos.financeiro.infrastructure.database.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Os índices UNIQUE em {@code email} e {@code google_id} são a garantia real de unicidade.
 * A checagem no serviço existe para dar uma mensagem decente; só o banco resolve a corrida
 * entre dois cadastros simultâneos com o mesmo e-mail.
 *
 * <p>{@code google_id} é nulo para quem só usa senha — e um índice UNIQUE aceita vários nulos,
 * tanto no Postgres quanto no H2.
 *
 * <p><strong>Índices e constraints não são declarados aqui.</strong> Eles vivem apenas nas
 * migrações Flyway ({@code db/migration}), que são a fonte única de verdade do esquema.
 * Com {@code ddl-auto=validate} as anotações {@code @Index} não teriam efeito nenhum — seriam
 * documentação capaz de mentir, já que o Hibernate não valida índices nem constraints.
 */
@Entity
@Table(name = "tb_users")
public class UserJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(length = 255)
    private String name;

    /** Hash BCrypt. Nulo para contas que só entram pelo Google. */
    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    /** O {@code sub} do token do Google. Nulo para contas que só usam senha. */
    @Column(name = "google_id", length = 64)
    private String googleId;

    @Column(name = "email_verified", nullable = false)
    private boolean emailVerified;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Version
    private Long version;

    public UserJpaEntity() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getGoogleId() { return googleId; }
    public void setGoogleId(String googleId) { this.googleId = googleId; }

    public boolean isEmailVerified() { return emailVerified; }
    public void setEmailVerified(boolean emailVerified) { this.emailVerified = emailVerified; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Sem setter de propósito: quem controla a versão é o Hibernate. */
    public Long getVersion() { return version; }
}
