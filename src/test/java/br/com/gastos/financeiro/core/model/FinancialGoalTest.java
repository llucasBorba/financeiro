package br.com.gastos.financeiro.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialGoalTest {

    private static final UUID USER_ID = UUID.randomUUID();

    private FinancialGoal novaMeta() {
        return new FinancialGoal(null, USER_ID, "Viagem",
                new Money(new BigDecimal("5000.00"), "BRL"), LocalDate.of(2026, 12, 31));
    }

    @Test
    @DisplayName("começa zerada e não atingida")
    void startsEmpty() {
        FinancialGoal goal = novaMeta();

        assertTrue(goal.getCurrentAmount().isZero());
        assertFalse(goal.isAchieved());
        assertEquals("BRL", goal.getCurrentAmount().getCurrency());
    }

    @Test
    @DisplayName("acumula os aportes")
    void accumulatesDeposits() {
        FinancialGoal goal = novaMeta();

        goal.deposit(new Money(new BigDecimal("1000.00"), "BRL"));
        goal.deposit(new Money(new BigDecimal("500.50"), "BRL"));

        assertEquals(0, new BigDecimal("1500.50").compareTo(goal.getCurrentAmount().getAmount()));
        assertFalse(goal.isAchieved());
    }

    @Test
    @DisplayName("considera atingida ao alcançar o valor alvo")
    void detectsAchievement() {
        FinancialGoal goal = novaMeta();

        goal.deposit(new Money(new BigDecimal("5000.00"), "BRL"));

        assertTrue(goal.isAchieved());
    }

    @Test
    @DisplayName("recusa aporte zerado, nulo ou em outra moeda")
    void rejectsInvalidDeposits() {
        FinancialGoal goal = novaMeta();

        assertThrows(NullPointerException.class, () -> goal.deposit(null));
        assertThrows(IllegalArgumentException.class, () -> goal.deposit(Money.zero("BRL")));
        assertThrows(IllegalArgumentException.class, () -> goal.deposit(new Money(BigDecimal.TEN, "USD")));
    }

    @Test
    @DisplayName("exige os campos obrigatórios")
    void requiresMandatoryFields() {
        Money target = new Money(BigDecimal.TEN, "BRL");

        assertThrows(NullPointerException.class, () -> new FinancialGoal(null, null, "x", target, null));
        assertThrows(NullPointerException.class, () -> new FinancialGoal(null, USER_ID, null, target, null));
        assertThrows(NullPointerException.class, () -> new FinancialGoal(null, USER_ID, "x", null, null));
    }

    @Test
    @DisplayName("reconstitui a meta preservando o saldo acumulado")
    void reconstitutePreservesBalance() {
        FinancialGoal goal = FinancialGoal.reconstitute(UUID.randomUUID(), USER_ID, "Carro",
                new Money(new BigDecimal("30000.00"), "BRL"),
                new Money(new BigDecimal("7500.00"), "BRL"),
                LocalDate.of(2027, 6, 1));

        assertEquals(0, new BigDecimal("7500.00").compareTo(goal.getCurrentAmount().getAmount()));
        assertFalse(goal.isAchieved());
    }

    @Test
    @DisplayName("resgate reduz o valor guardado")
    void resgateReduzOSaldo() {
        FinancialGoal meta = novaMeta();
        meta.deposit(new Money(new BigDecimal("500.00"), "BRL"));

        meta.withdraw(new Money(new BigDecimal("200.00"), "BRL"));

        assertEquals(new BigDecimal("300.00"), meta.getCurrentAmount().getAmount());
    }

    @Test
    @DisplayName("recusa resgate maior que o saldo guardado")
    void recusaResgateAcimaDoSaldo() {
        FinancialGoal meta = novaMeta();
        meta.deposit(new Money(new BigDecimal("100.00"), "BRL"));

        // Quem barra é o Money: o resultado seria negativo, e quantia negativa nao existe.
        assertThrows(IllegalArgumentException.class,
                () -> meta.withdraw(new Money(new BigDecimal("100.01"), "BRL")));

        // E o saldo continua intacto: a operacao foi recusada, nao aplicada pela metade.
        assertEquals(new BigDecimal("100.00"), meta.getCurrentAmount().getAmount());
    }

    @Test
    @DisplayName("recusa resgate de valor zero")
    void recusaResgateZerado() {
        FinancialGoal meta = novaMeta();
        meta.deposit(new Money(new BigDecimal("100.00"), "BRL"));

        assertThrows(IllegalArgumentException.class,
                () -> meta.withdraw(Money.zero("BRL")));
    }

    @Test
    @DisplayName("resgate ate zerar e permitido")
    void permiteResgatarTudo() {
        FinancialGoal meta = novaMeta();
        meta.deposit(new Money(new BigDecimal("100.00"), "BRL"));

        meta.withdraw(new Money(new BigDecimal("100.00"), "BRL"));

        assertTrue(meta.getCurrentAmount().isZero());
    }
}
