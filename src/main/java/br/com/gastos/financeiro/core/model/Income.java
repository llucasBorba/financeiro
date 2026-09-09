package br.com.gastos.financeiro.core.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

/**
 * Dinheiro que entrou — salário, freela, o pix inesperado do seu pai.
 *
 * <p>Deliberadamente mais simples que {@link Expense}, e as ausências são a parte interessante:
 *
 * <ul>
 *   <li><strong>Sem status.</strong> Despesa tem ciclo "a vencer → paga" porque a conta chega
 *       antes do pagamento. Receita você registra depois que caiu na conta. Previsão de
 *       entrada é outro problema — aditivo, se um dia fizer falta.</li>
 *   <li><strong>Sem categoria.</strong> Categoria existe para <em>agregar</em>, e agregar só
 *       faz sentido quando há lançamentos demais para ler. Com 2 ou 3 receitas por mês, a
 *       lista já é o relatório.</li>
 * </ul>
 *
 * <p>Por isso a descrição é <strong>obrigatória</strong> aqui e opcional na despesa: sem
 * categoria, ela é a única identidade do lançamento.
 */
public class Income {

    public static final int MAX_DESCRIPTION_LENGTH = 255;

    private final UUID id;
    private final UUID userId;
    private Money amount;
    private String description;
    private LocalDate receivedAt;

    private Income(UUID id, UUID userId, Money amount, String description, LocalDate receivedAt) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.amount = Objects.requireNonNull(amount, "O valor da receita é obrigatório.");
        this.description = normalizeDescription(description);
        this.receivedAt = DateBounds.require(
                Objects.requireNonNull(receivedAt, "A data de recebimento é obrigatória."),
                "A data de recebimento");
    }

    public static Income create(UUID userId, Money amount, String description, LocalDate receivedAt) {
        return new Income(null, userId, amount, description, receivedAt);
    }

    /** Reconstituição a partir da persistência. */
    public static Income reconstitute(UUID id, UUID userId, Money amount, String description,
                                      LocalDate receivedAt) {
        return new Income(id, userId, amount, description, receivedAt);
    }

    /** Corrige os dados. As invariantes são as mesmas da criação. */
    public void update(Money amount, String description, LocalDate receivedAt) {
        this.amount = Objects.requireNonNull(amount, "O valor da receita é obrigatório.");
        this.description = normalizeDescription(description);
        this.receivedAt = DateBounds.require(
                Objects.requireNonNull(receivedAt, "A data de recebimento é obrigatória."),
                "A data de recebimento");
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    private static String normalizeDescription(String description) {
        Objects.requireNonNull(description, "A descrição é obrigatória.");
        String trimmed = description.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("A descrição é obrigatória.");
        }
        if (trimmed.length() > MAX_DESCRIPTION_LENGTH) {
            throw new IllegalArgumentException(
                    "A descrição deve ter no máximo " + MAX_DESCRIPTION_LENGTH + " caracteres.");
        }
        return trimmed;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public LocalDate getReceivedAt() { return receivedAt; }
}
