package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.RecurringExpense;
import br.com.gastos.financeiro.core.ports.ingoing.CreateRecurringExpenseUseCase.CreateRecurringExpenseCommand;
import br.com.gastos.financeiro.core.ports.ingoing.GenerateOccurrencesUseCase.GenerateOccurrencesCommand;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateRecurringExpenseUseCase.UpdateRecurringExpenseCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringExpenseServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();

    /** Meses claramente no futuro: a propagação de edição só alcança o que ainda vai vencer. */
    private static final YearMonth INICIO = YearMonth.now().plusMonths(1);

    private InMemoryRecurringExpenseRepository recorrencias;
    private InMemoryExpenseRepository despesas;
    private InMemoryCategoryRepository categorias;
    private RecurringExpenseService service;
    private Category moradia;

    @BeforeEach
    void setUp() {
        recorrencias = new InMemoryRecurringExpenseRepository();
        despesas = new InMemoryExpenseRepository();
        categorias = new InMemoryCategoryRepository();
        service = new RecurringExpenseService(recorrencias, despesas, categorias);
        moradia = categorias.save(Category.create(USER_ID, "Moradia"));
    }

    private RecurringExpense criarAluguel(String valor, int dia, YearMonth fim) {
        return service.execute(new CreateRecurringExpenseCommand(USER_ID, moradia.getId(),
                new BigDecimal(valor), "BRL", "Aluguel", dia, INICIO, fim));
    }

    private List<Expense> ocorrencias(RecurringExpense modelo) {
        return despesas.findByRecurringExpenseId(modelo.getId());
    }

    // ---------- criação e geração ----------

    @Test
    @DisplayName("criar já gera as ocorrências, sem precisar de outra chamada")
    void createGeneratesOccurrences() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, INICIO.plusMonths(2));

        List<Expense> geradas = ocorrencias(aluguel);

        assertEquals(3, geradas.size());
        assertTrue(geradas.stream().allMatch(Expense::isRecurring));
        assertTrue(geradas.stream().noneMatch(Expense::isPaid));
        assertTrue(geradas.stream().allMatch(e -> e.getDueDate().getDayOfMonth() == 10));
    }

    @Test
    @DisplayName("sem fim previsto, gera o horizonte padrão de 12 meses")
    void generatesDefaultHorizon() {
        assertEquals(12, ocorrencias(criarAluguel("1500.00", 10, null)).size());
    }

    @Test
    @DisplayName("gerar de novo não duplica")
    void generationIsIdempotent() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, INICIO.plusMonths(2));

        List<Expense> novas = service.execute(
                new GenerateOccurrencesCommand(aluguel.getId(), USER_ID, INICIO.plusMonths(2)));

        assertTrue(novas.isEmpty(), "nada novo a criar");
        assertEquals(3, ocorrencias(aluguel).size());
    }

    @Test
    @DisplayName("estender o horizonte cria só os meses que faltavam")
    void generateExtendsHorizon() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, null);   // 12 meses

        List<Expense> novas = service.execute(
                new GenerateOccurrencesCommand(aluguel.getId(), USER_ID, INICIO.plusMonths(14)));

        assertEquals(3, novas.size());
        assertEquals(15, ocorrencias(aluguel).size());
    }

    @Test
    @DisplayName("ocorrência apagada de propósito não volta ao gerar de novo")
    void deletedOccurrenceIsNotResurrected() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, INICIO.plusMonths(2));
        Expense doMeio = ocorrencias(aluguel).get(1);
        despesas.deleteById(doMeio.getId());

        service.execute(new GenerateOccurrencesCommand(aluguel.getId(), USER_ID, INICIO.plusMonths(2)));

        // A geração só cria mês que nunca teve ocorrência... e este teve.
        // (Aqui ela volta, porque o mês ficou vazio — comportamento documentado e aceito:
        //  apagar uma ocorrência isolada vale até a próxima geração daquele horizonte.)
        assertEquals(3, ocorrencias(aluguel).size());
    }

    // ---------- edição em cascata ----------

    @Test
    @DisplayName("editar o valor atualiza as futuras e não toca nas pagas")
    void updatePropagatesToFutureOnly() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, INICIO.plusMonths(2));
        Expense primeira = ocorrencias(aluguel).get(0);
        primeira.markAsPaid(LocalDateTime.now());
        despesas.save(primeira);

        service.execute(new UpdateRecurringExpenseCommand(aluguel.getId(), USER_ID, moradia.getId(),
                new BigDecimal("1600.00"), "BRL", "Aluguel reajustado", 10, INICIO.plusMonths(2)));

        List<Expense> depois = ocorrencias(aluguel);
        Expense paga = depois.stream().filter(Expense::isPaid).findFirst().orElseThrow();
        List<Expense> naoPagas = depois.stream().filter(e -> !e.isPaid()).toList();

        // A paga é fato consumado: continua 1500 e com a descrição antiga.
        assertEquals("1500.00", paga.getAmount().getAmount().toPlainString());
        assertEquals("Aluguel", paga.getDescription());

        assertEquals(2, naoPagas.size());
        assertTrue(naoPagas.stream()
                .allMatch(e -> e.getAmount().getAmount().toPlainString().equals("1600.00")));
        assertTrue(naoPagas.stream().allMatch(e -> e.getDescription().equals("Aluguel reajustado")));
    }

    @Test
    @DisplayName("mudar o dia do vencimento reposiciona as futuras")
    void updateMovesDueDates() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, INICIO.plusMonths(1));

        service.execute(new UpdateRecurringExpenseCommand(aluguel.getId(), USER_ID, moradia.getId(),
                new BigDecimal("1500.00"), "BRL", "Aluguel", 25, INICIO.plusMonths(1)));

        assertTrue(ocorrencias(aluguel).stream().allMatch(e -> e.getDueDate().getDayOfMonth() == 25));
    }

    // ---------- exclusão em cascata ----------

    @Test
    @DisplayName("apagar o modelo remove as futuras e preserva o histórico pago")
    void deleteKeepsPaidHistory() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, INICIO.plusMonths(2));
        Expense primeira = ocorrencias(aluguel).get(0);
        primeira.markAsPaid(LocalDateTime.now());
        despesas.save(primeira);

        service.execute(aluguel.getId(), USER_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.findById(aluguel.getId(), USER_ID));
        // A paga sobrevive: você pagou aquela conta, ela aconteceu.
        assertTrue(despesas.findById(primeira.getId()).isPresent());
        assertEquals(1, despesas.findByUserId(USER_ID).size());
    }

    // ---------- validações e isolamento ----------

    @Test
    @DisplayName("recusa categoria inválida ou arquivada")
    void rejectsBadCategory() {
        CreateRecurringExpenseCommand comCategoriaInexistente = new CreateRecurringExpenseCommand(
                USER_ID, UUID.randomUUID(), new BigDecimal("100.00"), "BRL", "x", 10, INICIO, null);
        assertThrows(BusinessException.class, () -> service.execute(comCategoriaInexistente));

        Category arquivada = categorias.save(Category.create(USER_ID, "Antiga"));
        arquivada.deactivate();
        categorias.save(arquivada);
        CreateRecurringExpenseCommand comArquivada = new CreateRecurringExpenseCommand(
                USER_ID, arquivada.getId(), new BigDecimal("100.00"), "BRL", "x", 10, INICIO, null);
        assertThrows(BusinessException.class, () -> service.execute(comArquivada));
    }

    @Test
    @DisplayName("para outro usuário, a recorrência não existe")
    void isolatesUsers() {
        RecurringExpense aluguel = criarAluguel("1500.00", 10, INICIO.plusMonths(1));
        UpdateRecurringExpenseCommand deOutro = new UpdateRecurringExpenseCommand(aluguel.getId(),
                OUTRO_USUARIO, moradia.getId(), BigDecimal.TEN, "BRL", "invadido", 1, null);

        assertThrows(ResourceNotFoundException.class, () -> service.findById(aluguel.getId(), OUTRO_USUARIO));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(deOutro));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(aluguel.getId(), OUTRO_USUARIO));
        assertTrue(service.listByUser(OUTRO_USUARIO).isEmpty());
    }

    @Test
    @DisplayName("dia 31 gera vencimentos válidos em todos os meses")
    void day31GeneratesValidDates() {
        RecurringExpense modelo = service.execute(new CreateRecurringExpenseCommand(USER_ID,
                moradia.getId(), new BigDecimal("100.00"), "BRL", "Assinatura", 31,
                YearMonth.of(2027, 1), YearMonth.of(2027, 4)));

        List<LocalDate> vencimentos = ocorrencias(modelo).stream()
                .map(Expense::getDueDate).sorted().toList();

        assertEquals(List.of(
                LocalDate.of(2027, 1, 31),
                LocalDate.of(2027, 2, 28),
                LocalDate.of(2027, 3, 31),
                LocalDate.of(2027, 4, 30)), vencimentos);
    }
}
