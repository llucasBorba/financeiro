package br.com.gastos.financeiro.core.model;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Usuário do sistema — a âncora de identidade a que despesas e metas pertencem.
 *
 * <p>Um usuário pode entrar por senha, pelo Google, ou pelos dois. A invariante central é que
 * ele precisa ter <strong>ao menos uma</strong> forma de login: uma conta sem senha e sem
 * conta Google vinculada seria inacessível para sempre.
 *
 * <p>O domínio guarda apenas o <em>hash</em> da senha e nunca a senha em claro. Ele também não
 * sabe qual algoritmo gerou esse hash — isso é responsabilidade do
 * {@link br.com.gastos.financeiro.core.ports.outgoing.PasswordHasherPort}.
 */
public class User {

    private final UUID id;
    private final Email email;
    private String name;
    private String passwordHash;
    private String googleId;
    /**
     * Se alguém já confirmou que este endereço existe e pertence ao dono da conta.
     *
     * <p>Contas criadas pelo Google nascem verificadas — o Google confirmou o endereço e nos
     * conta isso no claim {@code email_verified}. Contas criadas por senha nascem <em>não</em>
     * verificadas, porque hoje não há fluxo de confirmação. O campo existe desde já porque é um
     * fato que já conhecemos no momento da criação: descartá-lo seria perder informação de graça.
     */
    private boolean emailVerified;
    private final LocalDateTime createdAt;

    private User(UUID id, Email email, String name, String passwordHash, String googleId,
                 boolean emailVerified, LocalDateTime createdAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.email = Objects.requireNonNull(email, "O e-mail é obrigatório.");
        this.name = name;
        this.passwordHash = passwordHash;
        this.googleId = googleId;
        this.emailVerified = emailVerified;
        this.createdAt = createdAt != null ? createdAt : LocalDateTime.now();

        if (passwordHash == null && googleId == null) {
            throw new IllegalArgumentException("O usuário precisa de ao menos uma forma de login.");
        }
    }

    /** Cadastro por e-mail e senha. O hash já vem pronto — o domínio não conhece o algoritmo. */
    public static User withPassword(Email email, String name, String passwordHash) {
        Objects.requireNonNull(passwordHash, "O hash da senha é obrigatório.");
        return new User(null, email, name, passwordHash, null, false, null);
    }

    /** Primeiro login via Google: a conta nasce sem senha. */
    public static User withGoogle(Email email, String name, String googleId) {
        Objects.requireNonNull(googleId, "O id do Google é obrigatório.");
        return new User(null, email, name, null, googleId, true, null);
    }

    /** Reconstituição a partir da persistência, sem passar de novo pelas regras de criação. */
    public static User reconstitute(UUID id, Email email, String name, String passwordHash,
                                    String googleId, boolean emailVerified, LocalDateTime createdAt) {
        return new User(id, email, name, passwordHash, googleId, emailVerified, createdAt);
    }

    /**
     * Vincula uma conta Google a um usuário que já existe.
     *
     * <p>Recusa trocar um vínculo existente por outro: se a conta já aponta para um id do
     * Google, aceitar um id diferente entregaria o acesso a outra pessoa.
     */
    public void linkGoogle(String googleId) {
        Objects.requireNonNull(googleId, "O id do Google é obrigatório.");
        if (this.googleId != null && !this.googleId.equals(googleId)) {
            throw new IllegalStateException("Esta conta já está vinculada a outra conta Google.");
        }
        this.googleId = googleId;
        // Vincular só acontece quando o Google confirmou o endereço, então a conta passa a ser verificada.
        this.emailVerified = true;
    }

    public void changeName(String name) {
        this.name = name;
    }

    public boolean hasPassword() {
        return passwordHash != null;
    }

    public boolean isEmailVerified() {
        return emailVerified;
    }

    public UUID getId() { return id; }
    public Email getEmail() { return email; }
    public String getName() { return name; }
    public String getPasswordHash() { return passwordHash; }
    public String getGoogleId() { return googleId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
