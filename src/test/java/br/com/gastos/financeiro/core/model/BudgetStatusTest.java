package br.com.gastos.financeiro.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetStatusTest {

    private static final UUID USER = UUID.randomUUID();
    private static final UUID ALIMENTACAO = UUID.randomUUID();
    private static final UUID TRANSPORTE = UUID.randomUUID();
    private static final Map<UUID, String> NOMES =
            Map.of(ALIMENTACAO, "Alimentação", TRANSPORTE, "Transporte");

    private Budget limite(UUID categoria, String valor) {
        return new Budget(null, USER, categoria, new Money(new BigDecimal(valor), "BRL"));
    }

    private Expense despesa(UUID categoria, String valor) {
        return new Expense(null, USER, categoria, new Money(new BigDecimal(valor), "BRL"),
                "x", LocalDate.of(2026, 9, 10));
    }

    @Test
    @DisplayName("separa o que foi pago do que está a pagar")
    void separaPagoDeAPagar() {
        List<BudgetStatus> status = BudgetStatus.forMonth(
                List.of(limite(ALIMENTACAO, "1000.00")),
                List.of(despesa(ALIMENTACAO, "500.00")),
                List.of(despesa(ALIMENTACAO, "400.00")),
                NOMES);

        BudgetStatus alimentacao = status.get(0);
        assertEquals(new BigDecimal("500.00"), alimentacao.getSpent().getAmount());
        assertEquals(new BigDecimal("400.00"), alimentacao.getPending().getAmount());
        assertEquals(new BigDecimal("500.00"), alimentacao.remaining());
        assertEquals("Alimentação", alimentacao.getCategoryName());
    }

    @Test
    @DisplayName("a projeção avisa antes do estouro acontecer")
    void projecaoAvisaAntes() {
        // É o caso que motivou mostrar as duas bases: R$ 900 comprometidos num limite de
        // R$ 1.000, mas só R$ 500 pagos. Olhando só o pago, pareceria folgado.
        BudgetStatus s = BudgetStatus.forMonth(
                List.of(limite(ALIMENTACAO, "1000.00")),
                List.of(despesa(ALIMENTACAO, "500.00")),
                List.of(despesa(ALIMENTACAO, "450.00")),
                NOMES).get(0);

        assertEquals(new BigDecimal("50.00"), s.usedPercentage());
        assertEquals(new BigDecimal("95.00"), s.projectedPercentage());
        assertFalse(s.isExceeded(), "ainda não estourou com dinheiro que saiu");
        assertFalse(s.isProjectedToExceed(), "R$ 950 ainda cabe em R$ 1.000");
    }

    @Test
    @DisplayName("marca estouro projetado quando o comprometido passa do limite")
    void marcaEstouroProjetado() {
        BudgetStatus s = BudgetStatus.forMonth(
                List.of(limite(ALIMENTACAO, "1000.00")),
                List.of(despesa(ALIMENTACAO, "500.00")),
                List.of(despesa(ALIMENTACAO, "600.00")),
                NOMES).get(0);

        assertFalse(s.isExceeded(), "o que saiu ainda cabe");
        assertTrue(s.isProjectedToExceed(), "mas pagar tudo estoura");
    }

    @Test
    @DisplayName("estouro real deixa o restante negativo")
    void estouroRealDeixaRestanteNegativo() {
        BudgetStatus s = BudgetStatus.forMonth(
                List.of(limite(ALIMENTACAO, "1000.00")),
                List.of(despesa(ALIMENTACAO, "1200.00")),
                List.of(),
                NOMES).get(0);

        assertTrue(s.isExceeded());
        assertEquals(new BigDecimal("-200.00"), s.remaining());
        assertEquals(new BigDecimal("120.00"), s.usedPercentage());
    }

    @Test
    @DisplayName("ignora despesas de outra categoria")
    void ignoraOutrasCategorias() {
        BudgetStatus s = BudgetStatus.forMonth(
                List.of(limite(ALIMENTACAO, "1000.00")),
                List.of(despesa(ALIMENTACAO, "100.00"), despesa(TRANSPORTE, "900.00")),
                List.of(),
                NOMES).get(0);

        assertEquals(new BigDecimal("100.00"), s.getSpent().getAmount());
    }

    @Test
    @DisplayName("categoria com limite e nenhum gasto aparece zerada")
    void categoriaSemGastoApareceZerada() {
        // Justamente o caso que o byCategory do resumo NÃO cobre: aquele mapa é construído a
        // partir das despesas, então categoria sem gasto simplesmente não existiria lá.
        // Aqui a lista parte dos limites, não dos lançamentos.
        List<BudgetStatus> status = BudgetStatus.forMonth(
                List.of(limite(ALIMENTACAO, "1000.00")), List.of(), List.of(), NOMES);

        assertEquals(1, status.size());
        assertTrue(status.get(0).getSpent().isZero());
        assertEquals(new BigDecimal("1000.00"), status.get(0).remaining());
    }

    @Test
    @DisplayName("ordena do mais apertado para o mais folgado")
    void ordenaPeloMaisApertado() {
        List<BudgetStatus> status = BudgetStatus.forMonth(
                List.of(limite(ALIMENTACAO, "1000.00"), limite(TRANSPORTE, "1000.00")),
                List.of(despesa(ALIMENTACAO, "100.00"), despesa(TRANSPORTE, "900.00")),
                List.of(),
                NOMES);

        assertEquals("Transporte", status.get(0).getCategoryName());
        assertEquals("Alimentação", status.get(1).getCategoryName());
    }
}
