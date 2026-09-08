package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase.CreateGoalCommand;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase.DepositCommand;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateGoalUseCase.UpdateGoalCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FinancialGoalServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();

    private FinancialGoalService service;

    @BeforeEach
    void setUp() {
        service = new FinancialGoalService(new InMemoryFinancialGoalRepository());
    }

    private FinancialGoal criarMeta() {
        return service.execute(new CreateGoalCommand(USER_ID, "Reserva de emergência",
                new BigDecimal("10000.00"), "BRL", LocalDate.of(2027, 1, 1)));
    }

    @Test
    @DisplayName("cria a meta zerada")
    void createsGoal() {
        FinancialGoal goal = criarMeta();

        assertTrue(goal.getCurrentAmount().isZero());
        assertFalse(goal.isAchieved());
    }

    @Test
    @DisplayName("acumula aportes sucessivos")
    void accumulatesDeposits() {
        FinancialGoal goal = criarMeta();

        service.execute(new DepositCommand(goal.getId(), USER_ID, new BigDecimal("4000.00"), "BRL"));
        FinancialGoal atualizada = service.execute(
                new DepositCommand(goal.getId(), USER_ID, new BigDecimal("6000.00"), "BRL"));

        assertEquals(0, new BigDecimal("10000.00").compareTo(atualizada.getCurrentAmount().getAmount()));
        assertTrue(atualizada.isAchieved());
    }

    @Test
    @DisplayName("recusa aporte em moeda diferente da meta")
    void rejectsCurrencyMismatch() {
        FinancialGoal goal = criarMeta();
        DepositCommand emDolar = new DepositCommand(goal.getId(), USER_ID, new BigDecimal("100.00"), "USD");

        assertThrows(IllegalArgumentException.class, () -> service.execute(emDolar));
    }

    @Test
    @DisplayName("não deixa outro usuário aportar na meta")
    void blocksOtherUsers() {
        FinancialGoal goal = criarMeta();
        DepositCommand deOutro = new DepositCommand(goal.getId(), OUTRO_USUARIO, new BigDecimal("50.00"), "BRL");

        // Ver ExpenseServiceTest: posse violada responde "não encontrado", não "acesso negado".
        assertThrows(ResourceNotFoundException.class, () -> service.execute(deOutro));
        assertThrows(ResourceNotFoundException.class, () -> service.findById(goal.getId(), OUTRO_USUARIO));
    }

    @Test
    @DisplayName("informa quando a meta não existe")
    void reportsMissingGoal() {
        UUID inexistente = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () -> service.findById(inexistente, USER_ID));
    }

    @Test
    @DisplayName("lista apenas as metas do próprio usuário")
    void listsOnlyOwnGoals() {
        criarMeta();

        assertEquals(1, service.listByUser(USER_ID).size());
        assertTrue(service.listByUser(OUTRO_USUARIO).isEmpty());
    }

    // ---------- editar e excluir ----------

    @Test
    @DisplayName("edita a meta sem tocar no saldo acumulado")
    void updateKeepsAccumulatedBalance() {
        FinancialGoal meta = criarMeta();
        service.execute(new DepositCommand(meta.getId(), USER_ID, new BigDecimal("4000.00"), "BRL"));

        FinancialGoal editada = service.execute(new UpdateGoalCommand(meta.getId(), USER_ID,
                "Reserva maior", new BigDecimal("12000.00"), "BRL", LocalDate.of(2027, 6, 1)));

        assertEquals("Reserva maior", editada.getTitle());
        assertEquals(0, new BigDecimal("12000.00").compareTo(editada.getTargetAmount().getAmount()));
        // o aporte continua lá: saldo é resultado de depósitos, não campo que se digita
        assertEquals(0, new BigDecimal("4000.00").compareTo(editada.getCurrentAmount().getAmount()));
        assertFalse(editada.isAchieved());
    }

    @Test
    @DisplayName("recusa trocar a moeda de uma meta com saldo")
    void refusesCurrencyChange() {
        FinancialGoal meta = criarMeta();
        service.execute(new DepositCommand(meta.getId(), USER_ID, new BigDecimal("100.00"), "BRL"));

        UpdateGoalCommand emDolar = new UpdateGoalCommand(meta.getId(), USER_ID, "Reserva",
                new BigDecimal("2000.00"), "USD", null);

        // Senão a meta ficaria com R$ 100 guardados rumo a um alvo em dólares,
        // e todo aporte seguinte seria recusado por moedas diferentes.
        assertThrows(IllegalArgumentException.class, () -> service.execute(emDolar));
    }

    @Test
    @DisplayName("apaga a meta")
    void deletesGoal() {
        FinancialGoal meta = criarMeta();

        service.execute(meta.getId(), USER_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.findById(meta.getId(), USER_ID));
        assertTrue(service.listByUser(USER_ID).isEmpty());
    }

    @Test
    @DisplayName("outro usuário não edita nem apaga a meta alheia")
    void isolatesUpdateAndDelete() {
        FinancialGoal meta = criarMeta();
        UpdateGoalCommand deOutro = new UpdateGoalCommand(meta.getId(), OUTRO_USUARIO, "invadida",
                BigDecimal.TEN, "BRL", null);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(deOutro));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(meta.getId(), OUTRO_USUARIO));
        assertEquals("Reserva de emergência", service.findById(meta.getId(), USER_ID).getTitle());
    }
}
