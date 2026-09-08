package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.ports.ingoing.CreateIncomeUseCase.CreateIncomeCommand;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateIncomeUseCase.UpdateIncomeCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IncomeServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();

    private IncomeService service;

    @BeforeEach
    void setUp() {
        service = new IncomeService(new InMemoryIncomeRepository());
    }

    private Income criar(String valor, String descricao, LocalDate recebidoEm) {
        return service.execute(new CreateIncomeCommand(USER_ID, new BigDecimal(valor), "BRL",
                descricao, recebidoEm));
    }

    @Test
    @DisplayName("cria a receita normalizando moeda e escala")
    void createsIncome() {
        Income salario = service.execute(new CreateIncomeCommand(USER_ID, new BigDecimal("5000"),
                "brl", "Salário setembro", LocalDate.of(2026, 9, 5)));

        // O Money faz o trabalho: "brl" vira "BRL" e 5000 vira 5000.00.
        assertEquals("BRL", salario.getAmount().getCurrency());
        assertEquals("5000.00", salario.getAmount().getAmount().toPlainString());
    }

    @Test
    @DisplayName("recusa valor com mais casas decimais do que o banco guarda")
    void rejectsExcessivePrecision() {
        CreateIncomeCommand comando = new CreateIncomeCommand(USER_ID, new BigDecimal("100.555"),
                "BRL", "x", LocalDate.of(2026, 9, 5));

        assertThrows(IllegalArgumentException.class, () -> service.execute(comando));
    }

    @Test
    @DisplayName("edita a receita e a alteração persiste")
    void updatesIncome() {
        Income salario = criar("5000.00", "Salário", LocalDate.of(2026, 9, 5));

        Income editada = service.execute(new UpdateIncomeCommand(salario.getId(), USER_ID,
                new BigDecimal("5200.00"), "BRL", "Salário com bônus", LocalDate.of(2026, 9, 6)));

        assertEquals("Salário com bônus", editada.getDescription());
        assertEquals(LocalDate.of(2026, 9, 6), editada.getReceivedAt());
        assertEquals("Salário com bônus", service.findById(salario.getId(), USER_ID).getDescription());
    }

    @Test
    @DisplayName("apaga a receita")
    void deletesIncome() {
        Income pix = criar("300.00", "Pix do pai", LocalDate.of(2026, 9, 18));

        service.execute(pix.getId(), USER_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.findById(pix.getId(), USER_ID));
        assertTrue(service.listByUser(USER_ID).isEmpty());
    }

    @Test
    @DisplayName("lista em ordem decrescente de recebimento")
    void listsNewestFirst() {
        criar("5000.00", "Salário", LocalDate.of(2026, 9, 5));
        criar("300.00", "Pix do pai", LocalDate.of(2026, 9, 18));

        List<Income> recebidas = service.listByUser(USER_ID);

        assertEquals("Pix do pai", recebidas.get(0).getDescription());
        assertEquals("Salário", recebidas.get(1).getDescription());
    }

    @Test
    @DisplayName("filtra pelo período de recebimento")
    void filtersByPeriod() {
        criar("5000.00", "Salário setembro", LocalDate.of(2026, 9, 5));
        criar("5000.00", "Salário outubro", LocalDate.of(2026, 10, 5));

        List<Income> setembro = service.listByUserAndPeriod(USER_ID,
                LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30));

        assertEquals(1, setembro.size());
        assertEquals("Salário setembro", setembro.get(0).getDescription());
        assertEquals(2, service.listByUser(USER_ID).size());
    }

    @Test
    @DisplayName("recusa período com data inicial posterior à final")
    void rejectsInvertedPeriod() {
        LocalDate inicio = LocalDate.of(2026, 9, 30);
        LocalDate fim = LocalDate.of(2026, 9, 1);

        assertThrows(BusinessException.class, () -> service.listByUserAndPeriod(USER_ID, inicio, fim));
    }

    @Test
    @DisplayName("para outro usuário, a receita simplesmente não existe")
    void isolatesUsers() {
        Income minha = criar("5000.00", "Salário", LocalDate.of(2026, 9, 5));
        UpdateIncomeCommand deOutro = new UpdateIncomeCommand(minha.getId(), OUTRO_USUARIO,
                BigDecimal.TEN, "BRL", "invadida", LocalDate.of(2026, 9, 5));

        // 404 e não 403: um "acesso negado" confirmaria que este ID existe.
        assertThrows(ResourceNotFoundException.class, () -> service.findById(minha.getId(), OUTRO_USUARIO));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(deOutro));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(minha.getId(), OUTRO_USUARIO));
        assertTrue(service.listByUser(OUTRO_USUARIO).isEmpty());

        // e nada foi alterado
        assertEquals("Salário", service.findById(minha.getId(), USER_ID).getDescription());
    }
}
