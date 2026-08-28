package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;
import br.com.gastos.financeiro.core.model.enums.ExpenseType;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase.CreateExpenseCommand;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase.MarkAsPaidCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();

    private ExpenseService service;

    @BeforeEach
    void setUp() {
        service = new ExpenseService(new InMemoryExpenseRepository());
    }

    private Expense criarDespesa(LocalDate dueDate) {
        return service.execute(new CreateExpenseCommand(USER_ID, UUID.randomUUID(), new BigDecimal("120.00"),
                "BRL", "Conta de luz", dueDate, "variable"));
    }

    @Test
    @DisplayName("cria a despesa pendente aceitando o tipo em minúsculas")
    void createsExpense() {
        Expense expense = criarDespesa(LocalDate.of(2026, 5, 10));

        assertEquals(ExpenseStatus.PENDING, expense.getStatus());
        assertEquals(ExpenseType.VARIABLE, expense.getType());
        assertEquals("BRL", expense.getAmount().getCurrency());
    }

    @Test
    @DisplayName("rejeita tipo de despesa desconhecido ou ausente")
    void rejectsUnknownType() {
        CreateExpenseCommand invalido = new CreateExpenseCommand(USER_ID, null, BigDecimal.TEN, "BRL",
                "x", LocalDate.of(2026, 5, 10), "MENSAL");
        CreateExpenseCommand semTipo = new CreateExpenseCommand(USER_ID, null, BigDecimal.TEN, "BRL",
                "x", LocalDate.of(2026, 5, 10), null);

        assertThrows(BusinessException.class, () -> service.execute(invalido));
        assertThrows(BusinessException.class, () -> service.execute(semTipo));
    }

    @Test
    @DisplayName("marca a despesa como paga e persiste a alteração")
    void marksAsPaid() {
        Expense expense = criarDespesa(LocalDate.of(2026, 5, 10));
        LocalDateTime pagamento = LocalDateTime.of(2026, 5, 9, 10, 0);

        Expense paga = service.execute(new MarkAsPaidCommand(expense.getId(), USER_ID, pagamento));

        assertTrue(paga.isPaid());
        assertEquals(pagamento, paga.getPaidAt());
        assertTrue(service.findById(expense.getId(), USER_ID).isPaid());
    }

    @Test
    @DisplayName("não deixa outro usuário mexer na despesa")
    void blocksOtherUsers() {
        Expense expense = criarDespesa(LocalDate.of(2026, 5, 10));
        MarkAsPaidCommand command = new MarkAsPaidCommand(expense.getId(), OUTRO_USUARIO, null);

        assertThrows(BusinessException.class, () -> service.execute(command));
        assertThrows(BusinessException.class, () -> service.findById(expense.getId(), OUTRO_USUARIO));
    }

    @Test
    @DisplayName("informa quando a despesa não existe")
    void reportsMissingExpense() {
        UUID inexistente = UUID.randomUUID();

        assertThrows(ResourceNotFoundException.class, () -> service.findById(inexistente, USER_ID));
    }

    @Test
    @DisplayName("filtra as despesas pelo período de vencimento")
    void filtersByPeriod() {
        criarDespesa(LocalDate.of(2026, 5, 10));
        criarDespesa(LocalDate.of(2026, 7, 20));

        List<Expense> maio = service.listByUserAndPeriod(USER_ID,
                LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertEquals(1, maio.size());
        assertEquals(2, service.listByUser(USER_ID).size());
        assertTrue(service.listByUser(OUTRO_USUARIO).isEmpty());
    }

    @Test
    @DisplayName("recusa período com data inicial posterior à final")
    void rejectsInvertedPeriod() {
        LocalDate inicio = LocalDate.of(2026, 5, 31);
        LocalDate fim = LocalDate.of(2026, 5, 1);

        assertThrows(BusinessException.class, () -> service.listByUserAndPeriod(USER_ID, inicio, fim));
    }
}
