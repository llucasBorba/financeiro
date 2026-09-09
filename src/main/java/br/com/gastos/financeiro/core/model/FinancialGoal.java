package br.com.gastos.financeiro.core.model;

import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class FinancialGoal {

    private final UUID id;
    private final UUID userId;
    private String title;
    private Money targetAmount;
    private Money currentAmount;
    private LocalDate targetDate;

    public FinancialGoal(UUID id, UUID userId, String title, Money targetAmount, LocalDate targetDate) {
        this.id = id != null ? id : UUID.randomUUID();
        this.userId = Objects.requireNonNull(userId, "O usuário é obrigatório.");
        this.title = Objects.requireNonNull(title, "O título da meta é obrigatório.");
        this.targetAmount = Objects.requireNonNull(targetAmount, "O valor alvo é obrigatório.");
        this.currentAmount = Money.zero(targetAmount.getCurrency());
        this.targetDate = DateBounds.require(targetDate, "A data alvo");
    }

    /**
     * Construtor de reconstituição: usado apenas pela camada de persistência para
     * restaurar uma meta já existente com o saldo que ela tem hoje.
     */
    public static FinancialGoal reconstitute(UUID id, UUID userId, String title, Money targetAmount,
                                             Money currentAmount, LocalDate targetDate) {
        FinancialGoal goal = new FinancialGoal(id, userId, title, targetAmount, targetDate);
        goal.currentAmount = currentAmount != null ? currentAmount : Money.zero(targetAmount.getCurrency());
        return goal;
    }

    /**
     * Corrige os dados da meta.
     *
     * <p>Não toca em {@code currentAmount}, pelo mesmo motivo que editar despesa não mexe no
     * pagamento: o saldo acumulado é resultado de aportes, não um campo que se digita. Mudá-lo
     * como efeito colateral de corrigir um título apagaria histórico.
     *
     * <p>A moeda do valor alvo precisa continuar sendo a do saldo já acumulado — senão a meta
     * ficaria com R$ 4.000 guardados rumo a um alvo em dólares, e {@link #deposit(Money)}
     * passaria a recusar todo aporte.
     */
    public void update(String title, Money targetAmount, LocalDate targetDate) {
        Objects.requireNonNull(targetAmount, "O valor alvo é obrigatório.");

        if (!targetAmount.getCurrency().equals(this.currentAmount.getCurrency())) {
            throw new IllegalArgumentException(
                    "A moeda da meta não pode mudar: já há " + this.currentAmount + " acumulado.");
        }

        this.title = Objects.requireNonNull(title, "O título da meta é obrigatório.");
        this.targetAmount = targetAmount;
        this.targetDate = DateBounds.require(targetDate, "A data alvo");
    }

    // Regra de negócio: adicionar aporte à meta
    public void deposit(Money contribution) {
        Objects.requireNonNull(contribution, "O valor do aporte é obrigatório.");
        if (contribution.isZero()) {
            throw new IllegalArgumentException("O aporte deve ser maior que zero.");
        }
        this.currentAmount = this.currentAmount.add(contribution);
    }

    /**
     * Retira dinheiro da meta. Simétrico a {@link #deposit(Money)}.
     *
     * <p>Sem isto, guardar dinheiro numa meta era via de mão única: um aporte digitado errado
     * só poderia ser desfeito apagando a meta e recriando, perdendo o id e o histórico — a
     * mesma assimetria que {@code undoPayment()} resolveu na despesa.
     *
     * <p>Quem barra o resgate maior que o saldo é o próprio {@link Money}: subtrair além do
     * que existe produziria uma quantia negativa, que o value object não admite. A regra mora
     * lá porque vale para qualquer dinheiro do sistema, não só para metas.
     */
    public void withdraw(Money amount) {
        Objects.requireNonNull(amount, "O valor do resgate é obrigatório.");
        if (amount.isZero()) {
            throw new IllegalArgumentException("O resgate deve ser maior que zero.");
        }
        this.currentAmount = this.currentAmount.subtract(amount);
    }

    // Métodos utilitários de negócio
    public boolean isAchieved() {
        return this.currentAmount.getAmount().compareTo(this.targetAmount.getAmount()) >= 0;
    }

    public boolean isOwnedBy(UUID candidateUserId) {
        return this.userId.equals(candidateUserId);
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTitle() { return title; }
    public Money getTargetAmount() { return targetAmount; }
    public Money getCurrentAmount() { return currentAmount; }
    public LocalDate getTargetDate() { return targetDate; }
}
