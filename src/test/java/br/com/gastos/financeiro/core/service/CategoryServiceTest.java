package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.ingoing.ChangeCategoryStatusUseCase.ChangeCategoryStatusCommand;
import br.com.gastos.financeiro.core.ports.ingoing.CreateCategoryUseCase.CreateCategoryCommand;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase.CreateExpenseCommand;
import br.com.gastos.financeiro.core.ports.ingoing.RenameCategoryUseCase.RenameCategoryCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CategoryServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();
    private static final UUID OUTRO_USUARIO = UUID.randomUUID();

    private InMemoryCategoryRepository categoryRepository;
    private InMemoryExpenseRepository expenseRepository;
    private CategoryService service;
    private ExpenseService expenseService;

    @BeforeEach
    void setUp() {
        categoryRepository = new InMemoryCategoryRepository();
        expenseRepository = new InMemoryExpenseRepository();
        service = new CategoryService(categoryRepository, expenseRepository);
        expenseService = new ExpenseService(expenseRepository, categoryRepository);
    }

    private Category criar(String nome) {
        return service.execute(new CreateCategoryCommand(USER_ID, nome));
    }

    // ---------- criação ----------

    @Test
    @DisplayName("cria a categoria ativa e com o nome sem espaços nas bordas")
    void createsActiveCategory() {
        Category categoria = criar("  Alimentação  ");

        assertEquals("Alimentação", categoria.getName());
        assertTrue(categoria.isActive());
    }

    @Test
    @DisplayName("recusa nome repetido, mesmo em outra caixa")
    void rejectsDuplicateName() {
        criar("Alimentação");

        assertThrows(BusinessException.class,
                () -> service.execute(new CreateCategoryCommand(USER_ID, "alimentação")));
    }

    @Test
    @DisplayName("o nome só é único dentro do próprio usuário")
    void nameIsUniquePerUser() {
        criar("Alimentação");

        // Outro usuário pode ter uma categoria com o mesmo nome — não há colisão global.
        Category deOutro = service.execute(new CreateCategoryCommand(OUTRO_USUARIO, "Alimentação"));

        assertEquals("Alimentação", deOutro.getName());
    }

    @Test
    @DisplayName("recusa nome vazio ou longo demais")
    void rejectsInvalidName() {
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new CreateCategoryCommand(USER_ID, "   ")));
        assertThrows(IllegalArgumentException.class,
                () -> service.execute(new CreateCategoryCommand(USER_ID, "x".repeat(61))));
    }

    // ---------- renomear ----------

    @Test
    @DisplayName("renomear não desliga a despesa da categoria")
    void renameKeepsExpensesAttached() {
        Category categoria = criar("Uber");
        expenseService.execute(new CreateExpenseCommand(USER_ID, categoria.getId(),
                new BigDecimal("25.00"), "BRL", "corrida", LocalDate.of(2026, 5, 10), null));

        service.execute(new RenameCategoryCommand(categoria.getId(), USER_ID, "Transporte por app"));

        // A despesa aponta para o ID; o nome novo aparece sem nenhum UPDATE em cascata.
        assertEquals(1, expenseRepository.countByCategoryId(categoria.getId()));
        assertEquals("Transporte por app", service.findById(categoria.getId(), USER_ID).getName());
    }

    @Test
    @DisplayName("renomear para o próprio nome é permitido")
    void renameToSameNameIsAllowed() {
        Category categoria = criar("Lazer");

        Category renomeada = service.execute(new RenameCategoryCommand(categoria.getId(), USER_ID, "Lazer"));

        assertEquals("Lazer", renomeada.getName());
    }

    @Test
    @DisplayName("renomear para um nome já usado é recusado")
    void renameToExistingNameIsRejected() {
        criar("Lazer");
        Category outra = criar("Compras");

        assertThrows(BusinessException.class,
                () -> service.execute(new RenameCategoryCommand(outra.getId(), USER_ID, "Lazer")));
    }

    // ---------- arquivar ----------

    @Test
    @DisplayName("arquivada some da listagem padrão, mas continua acessível")
    void archivedDisappearsFromDefaultListing() {
        Category categoria = criar("Uber");

        service.execute(new ChangeCategoryStatusCommand(categoria.getId(), USER_ID, false));

        assertTrue(service.listByUser(USER_ID, false).isEmpty());
        assertEquals(1, service.listByUser(USER_ID, true).size());
        assertFalse(service.findById(categoria.getId(), USER_ID).isActive());
    }

    @Test
    @DisplayName("categoria arquivada não aceita despesa nova")
    void archivedRejectsNewExpense() {
        Category categoria = criar("Uber");
        service.execute(new ChangeCategoryStatusCommand(categoria.getId(), USER_ID, false));

        CreateExpenseCommand comando = new CreateExpenseCommand(USER_ID, categoria.getId(),
                BigDecimal.TEN, "BRL", "corrida", LocalDate.of(2026, 5, 10), null);

        assertThrows(BusinessException.class, () -> expenseService.execute(comando));
    }

    // ---------- excluir ----------

    @Test
    @DisplayName("exclui categoria sem uso")
    void deletesUnusedCategory() {
        Category categoria = criar("Engano");

        service.execute(categoria.getId(), USER_ID);

        assertThrows(ResourceNotFoundException.class, () -> service.findById(categoria.getId(), USER_ID));
    }

    @Test
    @DisplayName("recusa excluir categoria em uso e diz quantas despesas dependem dela")
    void refusesToDeleteCategoryInUse() {
        Category categoria = criar("Alimentação");
        expenseService.execute(new CreateExpenseCommand(USER_ID, categoria.getId(),
                BigDecimal.TEN, "BRL", "mercado", LocalDate.of(2026, 5, 10), null));

        IllegalStateException erro = assertThrows(IllegalStateException.class,
                () -> service.execute(categoria.getId(), USER_ID));

        assertTrue(erro.getMessage().contains("1 despesa"), erro.getMessage());
        assertTrue(erro.getMessage().contains("Arquive"), erro.getMessage());
    }

    // ---------- isolamento ----------

    @Test
    @DisplayName("um usuário não enxerga, edita nem apaga categoria do outro")
    void isolatesUsers() {
        Category minha = criar("Alimentação");

        assertThrows(ResourceNotFoundException.class, () -> service.findById(minha.getId(), OUTRO_USUARIO));
        assertThrows(ResourceNotFoundException.class,
                () -> service.execute(new RenameCategoryCommand(minha.getId(), OUTRO_USUARIO, "invadida")));
        assertThrows(ResourceNotFoundException.class, () -> service.execute(minha.getId(), OUTRO_USUARIO));
        assertTrue(service.listByUser(OUTRO_USUARIO, true).isEmpty());
    }

    // ---------- padrão do domínio ----------

    @Test
    @DisplayName("toda conta nova nasce com as categorias padrão")
    void defaultsAreProvided() {
        List<Category> padroes = Category.defaultsFor(USER_ID);

        assertEquals(8, padroes.size());
        assertTrue(padroes.stream().allMatch(Category::isActive));
        assertTrue(padroes.stream().anyMatch(c -> c.getName().equals("Alimentação")));
        assertTrue(padroes.stream().allMatch(c -> c.isOwnedBy(USER_ID)));
    }
}
