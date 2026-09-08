package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase.CreateExpenseCommand;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase.MarkAsPaidCommand;
import br.com.gastos.financeiro.core.ports.ingoing.UndoExpensePaymentUseCase.UndoPaymentCommand;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateExpenseUseCase.UpdateExpenseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();

    private ExpenseService service;
    private InMemoryCategoryRepository categoryRepository;
    private Category categoria;

    @BeforeEach
    void setUp() {
        categoryRepository = new InMemoryCategoryRepository();
        service = new ExpenseService(new InMemoryExpenseRepository(), categoryRepository);
        // Desde a Etapa 2 o categoryId precisa apontar para uma categoria real do usuário —
        // um UUID inventado é recusado, que é exatamente o comportamento desejado.
        categoria = categoryRepository.save(Category.create(USER_ID, "Casa"));
    }

    private Expense criarDespesa(LocalDate dueDate) {
        return service.execute(new CreateExpenseCommand(USER_ID, categoria.getId(), new BigDecimal("120.00"),
                "BRL", "Conta de luz", dueDate, null));
    }

    @Test
    @DisplayName("cria a despesa pendente aceitando o tipo em minúsculas")
    void createsExpense() {
        Expense expense = criarDespesa(LocalDate.of(2026, 5, 10));

        assertEquals(ExpenseStatus.PENDING, expense.getStatus());
        assertEquals("BRL", expense.getAmount().getCurrency());
    }


    @Test
    @DisplayName("despesa digitada à mão nasce avulsa, sem vínculo com recorrência")
    void manualExpenseIsNotRecurring() {
        Expense avulsa = criarDespesa(LocalDate.of(2026, 5, 10));

        // Substituiu o antigo type=VARIABLE: em vez de um rótulo declarado, um fato verificável.
        assertFalse(avulsa.isRecurring());
        assertNull(avulsa.getRecurringExpenseId());
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

        // Para quem não é dono, o recurso não existe — 404, não 403. Um "acesso negado"
        // confirmaria a existência do ID e viraria oráculo para descobrir dados alheios.
        assertThrows(ResourceNotFoundException.class, () -> service.execute(command));
        assertThrows(ResourceNotFoundException.class, () -> service.findById(expense.getId(), OUTRO_USUARIO));
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

    // ---------- edição ----------

    @Test
    @DisplayName("edita os dados da despesa")
    void updatesExpense() {
        Expense original = criarDespesa(LocalDate.of(2026, 5, 10));

        Expense editada = service.execute(new UpdateExpenseCommand(
                original.getId(), USER_ID, categoria.getId(), new BigDecimal("99.90"), "BRL",
                "Conta de luz corrigida", LocalDate.of(2026, 6, 15)));

        assertEquals(0, new BigDecimal("99.90").compareTo(editada.getAmount().getAmount()));
        assertEquals("Conta de luz corrigida", editada.getDescription());
        assertEquals(LocalDate.of(2026, 6, 15), editada.getDueDate());
        // e a alteração sobrevive à releitura
        assertEquals("Conta de luz corrigida", service.findById(original.getId(), USER_ID).getDescription());
    }

    @Test
    @DisplayName("editar não mexe no estado de pagamento")
    void updateDoesNotTouchPaymentState() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));
        LocalDateTime pagamento = LocalDateTime.of(2026, 5, 9, 10, 0);
        service.execute(new MarkAsPaidCommand(despesa.getId(), USER_ID, pagamento));

        Expense editada = service.execute(new UpdateExpenseCommand(
                despesa.getId(), USER_ID, categoria.getId(), new BigDecimal("50.00"), "BRL",
                "valor corrigido depois de pago", LocalDate.of(2026, 5, 10)));

        // Corrigir um valor digitado errado não pode "despagar" a conta.
        assertTrue(editada.isPaid());
        assertEquals(pagamento, editada.getPaidAt());
    }


    @Test
    @DisplayName("outro usuário não edita nem apaga a despesa")
    void blocksOtherUserFromUpdateAndDelete() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));
        UpdateExpenseCommand deOutro = new UpdateExpenseCommand(despesa.getId(), OUTRO_USUARIO, categoria.getId(),
                BigDecimal.TEN, "BRL", "invasao", LocalDate.of(2026, 5, 10));

        assertThrows(ResourceNotFoundException.class, () -> service.execute(deOutro));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(despesa.getId(), OUTRO_USUARIO));

        // e nada foi alterado
        assertEquals("Conta de luz", service.findById(despesa.getId(), USER_ID).getDescription());
    }

    // ---------- exclusão ----------

    @Test
    @DisplayName("apaga a despesa")
    void deletesExpense() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));

        service.execute(despesa.getId(), USER_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.findById(despesa.getId(), USER_ID));
        assertTrue(service.listByUser(USER_ID).isEmpty());
    }

    @Test
    @DisplayName("apagar despesa inexistente informa que não existe")
    void deletingMissingExpenseReports404() {
        assertThrows(ResourceNotFoundException.class, () -> service.execute(UUID.randomUUID(), USER_ID));
    }

    // ---------- vínculo com categoria ----------

    @Test
    @DisplayName("recusa despesa sem categoria")
    void rejectsExpenseWithoutCategory() {
        CreateExpenseCommand semCategoria = new CreateExpenseCommand(USER_ID, null,
                BigDecimal.TEN, "BRL", "avulsa", LocalDate.of(2026, 5, 10), null);

        // Sem categoria, o lançamento entraria no total do mês mas sumiria da quebra —
        // um buraco invisível no resumo. "Outros" existe justamente como saída sempre válida.
        assertThrows(BusinessException.class, () -> service.execute(semCategoria));
    }

    @Test
    @DisplayName("recusa categoria inexistente ou de outro usuário com a mesma mensagem")
    void rejectsInvalidCategory() {
        Category deOutro = categoryRepository.save(Category.create(OUTRO_USUARIO, "Alheia"));

        CreateExpenseCommand comInexistente = new CreateExpenseCommand(USER_ID, UUID.randomUUID(),
                BigDecimal.TEN, "BRL", "x", LocalDate.of(2026, 5, 10), null);
        CreateExpenseCommand comAlheia = new CreateExpenseCommand(USER_ID, deOutro.getId(),
                BigDecimal.TEN, "BRL", "x", LocalDate.of(2026, 5, 10), null);

        BusinessException inexistente = assertThrows(BusinessException.class, () -> service.execute(comInexistente));
        BusinessException alheia = assertThrows(BusinessException.class, () -> service.execute(comAlheia));

        // Mensagens diferentes revelariam quais IDs de categoria existem no sistema.
        assertEquals(inexistente.getMessage(), alheia.getMessage());
    }

    @Test
    @DisplayName("a edição valida a categoria como a criação")
    void updateValidatesCategoryToo() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));

        UpdateExpenseCommand comCategoriaInvalida = new UpdateExpenseCommand(despesa.getId(), USER_ID,
                UUID.randomUUID(), BigDecimal.TEN, "BRL", "x", LocalDate.of(2026, 5, 10));

        assertThrows(BusinessException.class, () -> service.execute(comCategoriaInvalida));
    }

    // ---------- compra à vista (nasce paga) ----------

    @Test
    @DisplayName("nasce paga quando o pagamento é informado na criação")
    void bornPaidWhenPaidAtIsGiven() {
        LocalDateTime compra = LocalDateTime.of(2026, 9, 2, 14, 30);

        Expense mercado = service.execute(new CreateExpenseCommand(USER_ID, categoria.getId(),
                new BigDecimal("120.00"), "BRL", "Mercado", LocalDate.of(2026, 9, 2), compra));

        assertTrue(mercado.isPaid());
        assertEquals(ExpenseStatus.PAID, mercado.getStatus());
        assertEquals(compra, mercado.getPaidAt());
    }

    @Test
    @DisplayName("sem paidAt continua nascendo pendente")
    void stillBornPendingWithoutPaidAt() {
        // Garante que a novidade não mudou o comportamento das contas que de fato vencem.
        Expense conta = criarDespesa(LocalDate.of(2026, 5, 10));

        assertFalse(conta.isPaid());
        assertNull(conta.getPaidAt());
    }

    // ---------- desfazer pagamento ----------

    @Test
    @DisplayName("desfaz o pagamento e devolve a despesa para 'a pagar'")
    void undoesPayment() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));
        service.execute(new MarkAsPaidCommand(despesa.getId(), USER_ID, LocalDateTime.of(2026, 5, 9, 10, 0)));

        Expense revertida = service.execute(new UndoPaymentCommand(despesa.getId(), USER_ID));

        assertFalse(revertida.isPaid());
        assertNull(revertida.getPaidAt());
        // e a reversão sobrevive à releitura
        assertFalse(service.findById(despesa.getId(), USER_ID).isPaid());
    }

    @Test
    @DisplayName("não desfaz pagamento de despesa que não está paga")
    void refusesToUndoUnpaidExpense() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));
        UndoPaymentCommand comando = new UndoPaymentCommand(despesa.getId(), USER_ID);

        assertThrows(IllegalStateException.class, () -> service.execute(comando));
    }

    @Test
    @DisplayName("pode pagar de novo depois de desfazer")
    void canPayAgainAfterUndo() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));
        service.execute(new MarkAsPaidCommand(despesa.getId(), USER_ID, null));
        service.execute(new UndoPaymentCommand(despesa.getId(), USER_ID));

        Expense repaga = service.execute(new MarkAsPaidCommand(despesa.getId(), USER_ID, null));

        assertTrue(repaga.isPaid());
    }

    @Test
    @DisplayName("outro usuário não desfaz o pagamento da despesa alheia")
    void otherUserCannotUndoPayment() {
        Expense despesa = criarDespesa(LocalDate.of(2026, 5, 10));
        service.execute(new MarkAsPaidCommand(despesa.getId(), USER_ID, null));
        UndoPaymentCommand deOutro = new UndoPaymentCommand(despesa.getId(), OUTRO_USUARIO);

        assertThrows(ResourceNotFoundException.class, () -> service.execute(deOutro));
        assertTrue(service.findById(despesa.getId(), USER_ID).isPaid());
    }
}
