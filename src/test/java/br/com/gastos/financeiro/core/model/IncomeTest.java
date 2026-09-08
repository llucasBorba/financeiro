package br.com.gastos.financeiro.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomeTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final LocalDate RECEBIDO_EM = LocalDate.of(2026, 9, 5);

    private static Money reais(String valor) {
        return new Money(new BigDecimal(valor), "BRL");
    }

    @Test
    @DisplayName("nasce com id gerado e descrição sem espaços nas bordas")
    void createsWithGeneratedId() {
        Income income = Income.create(USER_ID, reais("5000.00"), "  Salário setembro  ", RECEBIDO_EM);

        assertNotNull(income.getId());
        assertEquals("Salário setembro", income.getDescription());
        assertEquals(0, new BigDecimal("5000.00").compareTo(income.getAmount().getAmount()));
        assertEquals(RECEBIDO_EM, income.getReceivedAt());
    }

    @Test
    @DisplayName("exige os campos obrigatórios")
    void requiresMandatoryFields() {
        Money valor = reais("100.00");

        assertThrows(NullPointerException.class,
                () -> Income.create(null, valor, "x", RECEBIDO_EM));
        assertThrows(NullPointerException.class,
                () -> Income.create(USER_ID, null, "x", RECEBIDO_EM));
        assertThrows(NullPointerException.class,
                () -> Income.create(USER_ID, valor, null, RECEBIDO_EM));
        assertThrows(NullPointerException.class,
                () -> Income.create(USER_ID, valor, "x", null));
    }

    @Test
    @DisplayName("a descrição é obrigatória — é a única identidade do lançamento")
    void descriptionIsRequired() {
        Money valor = reais("100.00");

        // Diferente da despesa, que se identifica pela categoria: "R$ 300 em 18/09" não
        // diz nada sem a descrição.
        assertThrows(IllegalArgumentException.class,
                () -> Income.create(USER_ID, valor, "   ", RECEBIDO_EM));
        assertThrows(IllegalArgumentException.class,
                () -> Income.create(USER_ID, valor, "x".repeat(256), RECEBIDO_EM));
    }

    @Test
    @DisplayName("edita os dados mantendo id e dono")
    void updatesKeepingIdentity() {
        Income income = Income.create(USER_ID, reais("5000.00"), "Salário", RECEBIDO_EM);
        UUID idOriginal = income.getId();

        income.update(reais("5200.00"), "Salário com bônus", LocalDate.of(2026, 9, 6));

        assertEquals(idOriginal, income.getId());
        assertEquals("Salário com bônus", income.getDescription());
        assertEquals(0, new BigDecimal("5200.00").compareTo(income.getAmount().getAmount()));
        assertTrue(income.isOwnedBy(USER_ID));
    }

    @Test
    @DisplayName("identifica o dono")
    void identifiesOwner() {
        Income income = Income.create(USER_ID, reais("100.00"), "x", RECEBIDO_EM);

        assertTrue(income.isOwnedBy(USER_ID));
        assertFalse(income.isOwnedBy(UUID.randomUUID()));
    }
}
