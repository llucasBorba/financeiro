package br.com.gastos.financeiro.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class EmailTest {

    @Test
    @DisplayName("normaliza para minúsculas e sem espaços nas bordas")
    void normalizes() {
        assertEquals("lucas@exemplo.com", new Email("  Lucas@Exemplo.COM  ").value());
    }

    @Test
    @DisplayName("dois endereços equivalentes são o mesmo e-mail")
    void equivalentAddressesAreEqual() {
        // É disto que depende a unicidade da conta: sem normalizar, a mesma pessoa
        // conseguiria criar duas contas só mudando a caixa das letras.
        assertEquals(new Email("Lucas@Exemplo.com"), new Email("lucas@exemplo.com"));
        assertEquals(new Email("Lucas@Exemplo.com").hashCode(), new Email("lucas@exemplo.com").hashCode());
        assertNotEquals(new Email("lucas@exemplo.com"), new Email("outro@exemplo.com"));
    }

    @Test
    @DisplayName("rejeita endereços malformados")
    void rejectsMalformed() {
        assertThrows(IllegalArgumentException.class, () -> new Email("sem-arroba"));
        assertThrows(IllegalArgumentException.class, () -> new Email("sem@dominio"));
        assertThrows(IllegalArgumentException.class, () -> new Email("@exemplo.com"));
        assertThrows(IllegalArgumentException.class, () -> new Email("com espaco@exemplo.com"));
        assertThrows(IllegalArgumentException.class, () -> new Email("   "));
        assertThrows(NullPointerException.class, () -> new Email(null));
    }

    @Test
    @DisplayName("rejeita endereço acima do limite da RFC")
    void rejectsTooLong() {
        String longo = "a".repeat(250) + "@exemplo.com";
        assertThrows(IllegalArgumentException.class, () -> new Email(longo));
    }
}
