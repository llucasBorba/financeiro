package br.com.gastos.financeiro.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RecurringExpenseTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private static RecurringExpense aluguel(int dia, YearMonth inicio, YearMonth fim) {
        return RecurringExpense.create(USER_ID, CATEGORY_ID,
                new Money(new BigDecimal("1500.00"), "BRL"), "Aluguel", dia, inicio, fim);
    }

    // ---------- a regra do dia 31 ----------

    @Test
    @DisplayName("vencimento dia 31 gruda no último dia dos meses mais curtos")
    void clampsDayToEndOfShortMonths() {
        RecurringExpense modelo = aluguel(31, YearMonth.of(2026, 1), null);

        assertEquals(LocalDate.of(2026, 1, 31), modelo.dueDateFor(YearMonth.of(2026, 1)));
        assertEquals(LocalDate.of(2026, 2, 28), modelo.dueDateFor(YearMonth.of(2026, 2)));
        assertEquals(LocalDate.of(2026, 4, 30), modelo.dueDateFor(YearMonth.of(2026, 4)));
        assertEquals(LocalDate.of(2026, 3, 31), modelo.dueDateFor(YearMonth.of(2026, 3)));
    }

    @Test
    @DisplayName("em ano bissexto, fevereiro vai até 29")
    void handlesLeapYear() {
        RecurringExpense modelo = aluguel(31, YearMonth.of(2028, 1), null);

        assertEquals(LocalDate.of(2028, 2, 29), modelo.dueDateFor(YearMonth.of(2028, 2)));
    }

    @Test
    @DisplayName("dia que cabe em todo mês não é ajustado")
    void keepsSafeDays() {
        RecurringExpense modelo = aluguel(10, YearMonth.of(2026, 1), null);

        assertEquals(LocalDate.of(2026, 2, 10), modelo.dueDateFor(YearMonth.of(2026, 2)));
    }

    @Test
    @DisplayName("recusa dia fora do intervalo 1..31")
    void rejectsInvalidDay() {
        assertThrows(IllegalArgumentException.class, () -> aluguel(0, YearMonth.of(2026, 1), null));
        assertThrows(IllegalArgumentException.class, () -> aluguel(32, YearMonth.of(2026, 1), null));
    }

    // ---------- alcance da regra ----------

    @Test
    @DisplayName("cobre do mês inicial em diante, respeitando o fim")
    void coversTheRightMonths() {
        RecurringExpense modelo = aluguel(10, YearMonth.of(2026, 10), YearMonth.of(2026, 12));

        assertFalse(modelo.covers(YearMonth.of(2026, 9)));
        assertTrue(modelo.covers(YearMonth.of(2026, 10)));
        assertTrue(modelo.covers(YearMonth.of(2026, 12)));
        assertFalse(modelo.covers(YearMonth.of(2027, 1)));
    }

    @Test
    @DisplayName("sem fim previsto, o horizonte padrão são 12 meses")
    void defaultHorizonIsTwelveMonths() {
        RecurringExpense semFim = aluguel(10, YearMonth.of(2026, 10), null);

        assertEquals(YearMonth.of(2027, 9), semFim.defaultHorizon());
        assertEquals(12, semFim.monthsThrough(semFim.defaultHorizon()).size());
    }

    @Test
    @DisplayName("com fim antes do horizonte, para no fim")
    void stopsAtDeclaredEnd() {
        RecurringExpense curto = aluguel(10, YearMonth.of(2026, 10), YearMonth.of(2026, 12));

        assertEquals(YearMonth.of(2026, 12), curto.defaultHorizon());
        assertEquals(3, curto.monthsThrough(YearMonth.of(2030, 1)).size());
    }

    @Test
    @DisplayName("recusa mês final anterior ao inicial")
    void rejectsEndBeforeStart() {
        assertThrows(IllegalArgumentException.class,
                () -> aluguel(10, YearMonth.of(2026, 10), YearMonth.of(2026, 9)));
    }

    // ---------- a ocorrência gerada ----------

    @Test
    @DisplayName("a ocorrência nasce pendente, vinculada ao modelo")
    void occurrenceIsBornPendingAndLinked() {
        RecurringExpense modelo = aluguel(10, YearMonth.of(2026, 10), null);

        Expense outubro = modelo.occurrenceFor(YearMonth.of(2026, 10));

        assertEquals(LocalDate.of(2026, 10, 10), outubro.getDueDate());
        assertEquals(modelo.getId(), outubro.getRecurringExpenseId());
        assertTrue(outubro.isRecurring());
        assertFalse(outubro.isPaid());
        assertEquals("Aluguel", outubro.getDescription());
        assertEquals(CATEGORY_ID, outubro.getCategoryId());
    }

    @Test
    @DisplayName("a série gerada respeita o intervalo declarado")
    void generatesTheDeclaredRange() {
        RecurringExpense modelo = aluguel(15, YearMonth.of(2026, 11), YearMonth.of(2027, 1));

        List<YearMonth> meses = modelo.monthsThrough(YearMonth.of(2027, 6));

        assertEquals(List.of(YearMonth.of(2026, 11), YearMonth.of(2026, 12), YearMonth.of(2027, 1)), meses);
    }

    // ---------- proteção contra geração sem teto ----------

    @Test
    @DisplayName("recusa geração que abrangeria mais que o teto de meses")
    void rejectsGenerationBeyondSpanLimit() {
        RecurringExpense semFim = aluguel(10, YearMonth.of(2026, 1), null);

        // Antes desta guarda, o laço montava a lista até o alvo sem teto algum. Como YearMonth
        // vai até o ano 999999999, uma requisição autenticada qualquer derrubava a JVM.
        assertThrows(IllegalArgumentException.class,
                () -> semFim.monthsThrough(YearMonth.of(2100, 12)));
        assertThrows(IllegalArgumentException.class,
                () -> semFim.monthsThrough(YearMonth.of(2200, 12)));
    }

    @Test
    @DisplayName("aceita exatamente o teto e recusa um mês além")
    void spanLimitBoundary() {
        YearMonth inicio = YearMonth.of(2026, 1);
        RecurringExpense semFim = aluguel(10, inicio, null);

        YearMonth noLimite = inicio.plusMonths(RecurringExpense.MAX_GENERATION_SPAN_MONTHS - 1L);
        assertEquals(RecurringExpense.MAX_GENERATION_SPAN_MONTHS, semFim.monthsThrough(noLimite).size());

        assertThrows(IllegalArgumentException.class,
                () -> semFim.monthsThrough(noLimite.plusMonths(1)));
    }

    @Test
    @DisplayName("recusa meses absurdamente distantes em qualquer campo")
    void rejectsAbsurdlyDistantMonths() {
        YearMonth distante = YearMonth.of(999999999, 12);

        assertThrows(IllegalArgumentException.class, () -> aluguel(10, distante, null));
        assertThrows(IllegalArgumentException.class,
                () -> aluguel(10, YearMonth.of(2026, 1), distante));
        assertThrows(IllegalArgumentException.class,
                () -> aluguel(10, YearMonth.of(2026, 1), null).monthsThrough(distante));
    }

    @Test
    @DisplayName("alvo anterior ao início não gera nada, em vez de estourar")
    void targetBeforeStartYieldsNothing() {
        RecurringExpense modelo = aluguel(10, YearMonth.of(2026, 10), null);

        assertTrue(modelo.monthsThrough(YearMonth.of(2026, 5)).isEmpty());
    }

    @Test
    @DisplayName("o horizonte padrão nunca ultrapassa o limite aceito")
    void defaultHorizonStaysWithinRange() {
        RecurringExpense noLimite = aluguel(10, YearMonth.of(2200, 12), null);

        // Sem a limitação, o padrão cairia em 2201-11 e a própria geração o recusaria.
        assertEquals(YearMonth.of(2200, 12), noLimite.defaultHorizon());
        assertEquals(1, noLimite.monthsThrough(noLimite.defaultHorizon()).size());
    }
}
