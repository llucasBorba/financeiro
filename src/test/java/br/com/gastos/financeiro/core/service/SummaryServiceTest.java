package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.model.MonthlySummary;
import br.com.gastos.financeiro.core.model.Income;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummaryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();
    private static final YearMonth SETEMBRO = YearMonth.of(2026, 9);

    private InMemoryExpenseRepository expenses;
    private InMemoryIncomeRepository incomes;
    private InMemoryCategoryRepository categories;
    private SummaryService service;
    private Category moradia;
    private Category alimentacao;

    @BeforeEach
    void setUp() {
        expenses = new InMemoryExpenseRepository();
        incomes = new InMemoryIncomeRepository();
        categories = new InMemoryCategoryRepository();
        service = new SummaryService(expenses, incomes, categories);

        moradia = categories.save(Category.create(USER_ID, "Moradia"));
        alimentacao = categories.save(Category.create(USER_ID, "Alimentação"));
    }

    private static Money reais(String valor) {
        return new Money(new BigDecimal(valor), "BRL");
    }

    private void receita(String valor, LocalDate recebidoEm) {
        incomes.save(Income.create(USER_ID, reais(valor), "entrada", recebidoEm));
    }

    /** Despesa paga: informa o vencimento e a data de pagamento separadamente. */
    private void despesaPaga(Category categoria, String valor, LocalDate vencimento, LocalDateTime pagoEm) {
        Expense e = new Expense(null, USER_ID, categoria.getId(), reais(valor), "x", vencimento);
        e.markAsPaid(pagoEm);
        expenses.save(e);
    }

    private void despesaPendente(Category categoria, String valor, LocalDate vencimento) {
        expenses.save(new Expense(null, USER_ID, categoria.getId(), reais(valor), "x", vencimento));
    }

    private MonthlySummary setembro() {
        List<MonthlySummary> resumos = service.execute(USER_ID, SETEMBRO);
        assertEquals(1, resumos.size(), "esperava um fechamento só, em BRL");
        return resumos.get(0);
    }

    @Test
    @DisplayName("mês sem lançamento nenhum devolve fechamento zerado, não lista vazia")
    void emptyMonthReturnsZeroedSummary() {
        MonthlySummary resumo = setembro();

        assertEquals("BRL", resumo.getCurrency());
        assertTrue(resumo.getReceived().isZero());
        assertTrue(resumo.getPaid().isZero());
        assertEquals(0, BigDecimal.ZERO.compareTo(resumo.balance()));
        assertTrue(resumo.getByCategory().isEmpty());
    }

    @Test
    @DisplayName("soma entradas e saídas do mês")
    void sumsIncomeAndPaidExpenses() {
        receita("5000.00", LocalDate.of(2026, 9, 5));
        receita("300.00", LocalDate.of(2026, 9, 18));
        despesaPaga(moradia, "1500.00", LocalDate.of(2026, 9, 10), LocalDateTime.of(2026, 9, 10, 9, 0));
        despesaPaga(alimentacao, "900.00", LocalDate.of(2026, 9, 15), LocalDateTime.of(2026, 9, 15, 9, 0));

        MonthlySummary resumo = setembro();

        assertEquals("5300.00", resumo.getReceived().getAmount().toPlainString());
        assertEquals("2400.00", resumo.getPaid().getAmount().toPlainString());
        assertEquals(0, new BigDecimal("2900.00").compareTo(resumo.balance()));
        assertEquals(2, resumo.getReceivedCount());
        assertEquals(2, resumo.getPaidCount());
    }

    @Test
    @DisplayName("conta a despesa no mês em que foi PAGA, não no que venceu")
    void countsExpenseInThePaymentMonth() {
        // Venceu em agosto, paga em setembro: é saída de SETEMBRO.
        despesaPaga(moradia, "1000.00", LocalDate.of(2026, 8, 10), LocalDateTime.of(2026, 9, 3, 9, 0));
        // Venceu em setembro, paga em outubro: NÃO é saída de setembro.
        despesaPaga(alimentacao, "500.00", LocalDate.of(2026, 9, 20), LocalDateTime.of(2026, 10, 2, 9, 0));

        MonthlySummary resumo = setembro();

        assertEquals("1000.00", resumo.getPaid().getAmount().toPlainString());
        assertEquals(1, resumo.getPaidCount());
    }

    @Test
    @DisplayName("pendente aparece como 'a pagar' e fica fora do saldo")
    void pendingIsReportedButExcludedFromBalance() {
        receita("5000.00", LocalDate.of(2026, 9, 5));
        despesaPaga(moradia, "1500.00", LocalDate.of(2026, 9, 10), LocalDateTime.of(2026, 9, 10, 9, 0));
        despesaPendente(alimentacao, "800.00", LocalDate.of(2026, 9, 25));

        MonthlySummary resumo = setembro();

        assertEquals("800.00", resumo.getPending().getAmount().toPlainString());
        assertEquals(1, resumo.getPendingCount());
        // saldo ignora o pendente: o dinheiro ainda não saiu
        assertEquals(0, new BigDecimal("3500.00").compareTo(resumo.balance()));
        // previsto desconta
        assertEquals(0, new BigDecimal("2700.00").compareTo(resumo.projectedBalance()));
        // e a quebra por categoria segue o PAGO, não o pendente
        assertEquals(1, resumo.getByCategory().size());
        assertEquals("Moradia", resumo.getByCategory().get(0).categoryName());
    }

    @Test
    @DisplayName("saldo negativo é um resultado legítimo, não um erro")
    void balanceCanBeNegative() {
        receita("1000.00", LocalDate.of(2026, 9, 5));
        despesaPaga(moradia, "1500.00", LocalDate.of(2026, 9, 10), LocalDateTime.of(2026, 9, 10, 9, 0));

        // Gastou mais do que ganhou. O Money proíbe negativo — por isso o saldo é BigDecimal.
        assertEquals(0, new BigDecimal("-500.00").compareTo(setembro().balance()));
    }

    @Test
    @DisplayName("quebra por categoria vem ordenada do maior gasto, com percentual")
    void breakdownIsSortedWithPercentages() {
        despesaPaga(moradia, "1500.00", LocalDate.of(2026, 9, 10), LocalDateTime.of(2026, 9, 10, 9, 0));
        despesaPaga(alimentacao, "500.00", LocalDate.of(2026, 9, 12), LocalDateTime.of(2026, 9, 12, 9, 0));

        List<MonthlySummary.CategoryTotal> quebra = setembro().getByCategory();

        assertEquals("Moradia", quebra.get(0).categoryName());
        assertEquals(0, new BigDecimal("75.0").compareTo(quebra.get(0).percentage()));
        assertEquals("Alimentação", quebra.get(1).categoryName());
        assertEquals(0, new BigDecimal("25.0").compareTo(quebra.get(1).percentage()));
    }

    @Test
    @DisplayName("lançamentos nas bordas do mês entram")
    void includesBoundaryDays() {
        receita("100.00", LocalDate.of(2026, 9, 1));
        despesaPaga(moradia, "50.00", LocalDate.of(2026, 9, 30), LocalDateTime.of(2026, 9, 30, 23, 59));

        MonthlySummary resumo = setembro();

        assertEquals("100.00", resumo.getReceived().getAmount().toPlainString());
        assertEquals("50.00", resumo.getPaid().getAmount().toPlainString());
    }

    @Test
    @DisplayName("moedas diferentes geram um fechamento para cada")
    void oneSummaryPerCurrency() {
        incomes.save(Income.create(USER_ID, reais("5000.00"), "salário", LocalDate.of(2026, 9, 5)));
        incomes.save(Income.create(USER_ID, new Money(new BigDecimal("200.00"), "USD"),
                "freela lá fora", LocalDate.of(2026, 9, 10)));

        List<MonthlySummary> resumos = service.execute(USER_ID, SETEMBRO);

        // Somar BRL com USD não faz sentido; em vez de escolher uma e esconder a outra,
        // cada moeda ganha seu fechamento.
        assertEquals(2, resumos.size());
        assertEquals("BRL", resumos.get(0).getCurrency());
        assertEquals("USD", resumos.get(1).getCurrency());
        assertEquals("200.00", resumos.get(1).getReceived().getAmount().toPlainString());
    }

    @Test
    @DisplayName("o resumo enxerga apenas os lançamentos do próprio usuário")
    void isolatesUsers() {
        receita("5000.00", LocalDate.of(2026, 9, 5));
        despesaPaga(moradia, "1500.00", LocalDate.of(2026, 9, 10), LocalDateTime.of(2026, 9, 10, 9, 0));

        List<MonthlySummary> deOutro = service.execute(OUTRO_USUARIO, SETEMBRO);

        assertEquals(1, deOutro.size());
        assertTrue(deOutro.get(0).getReceived().isZero());
        assertTrue(deOutro.get(0).getPaid().isZero());
    }
}
