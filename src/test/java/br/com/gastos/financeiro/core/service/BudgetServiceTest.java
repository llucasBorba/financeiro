package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Budget;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.ingoing.SetBudgetUseCase.SetBudgetCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BudgetServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();

    private InMemoryBudgetRepository budgetRepository;
    private InMemoryCategoryRepository categoryRepository;
    private BudgetService service;

    @BeforeEach
    void setUp() {
        budgetRepository = new InMemoryBudgetRepository();
        categoryRepository = new InMemoryCategoryRepository();
        service = new BudgetService(budgetRepository, categoryRepository, new InMemoryExpenseRepository());
    }

    private Category categoria(UUID dono, String nome) {
        return categoryRepository.save(Category.create(dono, nome));
    }

    private Budget definir(UUID categoriaId, String valor) {
        return service.execute(new SetBudgetCommand(USER_ID, categoriaId, new BigDecimal(valor), "BRL"));
    }

    @Test
    @DisplayName("definir duas vezes atualiza o mesmo orçamento")
    void definirDuasVezesAtualiza() {
        UUID categoria = categoria(USER_ID, "Alimentação").getId();

        Budget primeiro = definir(categoria, "1000.00");
        Budget segundo = definir(categoria, "1500.00");

        assertEquals(primeiro.getId(), segundo.getId(), "deve ser o mesmo registro, não um novo");
        assertEquals(1, budgetRepository.findByUserId(USER_ID).size());
        assertEquals(new BigDecimal("1500.00"), segundo.getMonthlyLimit().getAmount());
    }

    @Test
    @DisplayName("recusa categoria de outro dono com a mesma mensagem de categoria inexistente")
    void recusaCategoriaDeOutroDono() {
        UUID alheia = categoria(OUTRO_USUARIO, "Alimentação").getId();

        BusinessException erro = assertThrows(BusinessException.class, () -> definir(alheia, "100.00"));

        // Mesma mensagem que uma categoria inexistente produz: distinguir as duas daria ao
        // atacante um jeito de descobrir quais ids existem.
        assertEquals("Categoria inválida.", erro.getMessage());
        assertEquals("Categoria inválida.",
                assertThrows(BusinessException.class,
                        () -> definir(UUID.randomUUID(), "100.00")).getMessage());
    }

    @Test
    @DisplayName("recusa orçamento em categoria arquivada")
    void recusaCategoriaArquivada() {
        Category arquivada = categoria(USER_ID, "Antiga");
        arquivada.deactivate();
        categoryRepository.save(arquivada);

        assertThrows(BusinessException.class, () -> definir(arquivada.getId(), "100.00"));
    }

    @Test
    @DisplayName("sem nenhum limite definido, a consulta devolve lista vazia")
    void semLimitesDevolveVazio() {
        assertTrue(service.execute(USER_ID, YearMonth.of(2026, 9)).isEmpty());
    }

    @Test
    @DisplayName("remover limite inexistente falha como recurso não encontrado")
    void removerInexistenteFalha() {
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(USER_ID, UUID.randomUUID()));
    }

    @Test
    @DisplayName("remover apaga só o limite pedido")
    void removerApagaSoOPedido() {
        UUID a = categoria(USER_ID, "Alimentação").getId();
        UUID b = categoria(USER_ID, "Transporte").getId();
        definir(a, "1000.00");
        definir(b, "500.00");

        service.execute(USER_ID, a);

        assertEquals(1, budgetRepository.findByUserId(USER_ID).size());
        assertTrue(budgetRepository.findByUserIdAndCategoryId(USER_ID, b).isPresent());
    }
}
