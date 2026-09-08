package br.com.gastos.financeiro.core.model;

import br.com.gastos.financeiro.core.model.enums.ExpenseStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExpenseTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID CATEGORY_ID = UUID.randomUUID();

    private Expense novaDespesa() {
        return new Expense(null, USER_ID, UUID.randomUUID(), new Money(new BigDecimal("199.90"), "BRL"),
                "Internet", LocalDate.of(2026, 3, 10));
    }

    @Test
    @DisplayName("nasce pendente e com id gerado")
    void startsPending() {
        Expense expense = novaDespesa();

        assertNotNull(expense.getId());
        assertEquals(ExpenseStatus.PENDING, expense.getStatus());
        assertNull(expense.getPaidAt());
        assertFalse(expense.isPaid());
    }

    @Test
    @DisplayName("exige os campos obrigatórios")
    void requiresMandatoryFields() {
        Money amount = new Money(BigDecimal.TEN, "BRL");
        LocalDate dueDate = LocalDate.of(2026, 3, 10);

        assertThrows(NullPointerException.class,
                () -> new Expense(null, null, CATEGORY_ID, amount, "x", dueDate));
        assertThrows(NullPointerException.class,
                () -> new Expense(null, USER_ID, CATEGORY_ID, null, "x", dueDate));
        assertThrows(NullPointerException.class,
                () -> new Expense(null, USER_ID, CATEGORY_ID, amount, "x", null));
        assertThrows(NullPointerException.class,
                () -> new Expense(null, USER_ID, null, amount, "x", dueDate));
    }

    @Test
    @DisplayName("marca como paga na data informada")
    void marksAsPaid() {
        Expense expense = novaDespesa();
        LocalDateTime pagamento = LocalDateTime.of(2026, 3, 9, 14, 30);

        expense.markAsPaid(pagamento);

        assertTrue(expense.isPaid());
        assertEquals(pagamento, expense.getPaidAt());
    }

    @Test
    @DisplayName("usa o momento atual quando a data de pagamento é omitida")
    void marksAsPaidWithoutDate() {
        Expense expense = novaDespesa();

        expense.markAsPaid(null);

        assertNotNull(expense.getPaidAt());
    }

    @Test
    @DisplayName("não deixa pagar duas vezes")
    void rejectsDoublePayment() {
        Expense expense = novaDespesa();
        expense.markAsPaid(LocalDateTime.now());

        assertThrows(IllegalStateException.class, () -> expense.markAsPaid(LocalDateTime.now()));
    }

    @Test
    @DisplayName("reconstitui uma despesa paga sem sobrescrever a data de pagamento")
    void reconstitutePreservesPaidState() {
        LocalDateTime pagamento = LocalDateTime.of(2026, 1, 5, 8, 0);

        Expense expense = Expense.reconstitute(UUID.randomUUID(), USER_ID, UUID.randomUUID(),
                new Money(BigDecimal.TEN, "BRL"), "Luz", LocalDate.of(2026, 1, 10),
                ExpenseStatus.PAID, pagamento, null);

        assertTrue(expense.isPaid());
        assertEquals(pagamento, expense.getPaidAt());
    }

    @Test
    @DisplayName("identifica o dono")
    void checksOwnership() {
        Expense expense = novaDespesa();

        assertTrue(expense.isOwnedBy(USER_ID));
        assertFalse(expense.isOwnedBy(UUID.randomUUID()));
    }
}
