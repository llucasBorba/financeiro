package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Budget;
import br.com.gastos.financeiro.core.model.BudgetStatus;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteBudgetUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.ListBudgetStatusUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.SetBudgetUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.BudgetRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Casos de uso de orçamento por categoria.
 *
 * <p>A leitura busca os lançamentos com exatamente as mesmas duas consultas do
 * {@link SummaryService}, de propósito: se as definições de "pago" e "a pagar" divergissem
 * entre o resumo e o orçamento, o usuário veria dois números diferentes para a mesma coisa na
 * mesma tela, e nenhum dos dois pareceria confiável.
 */
public class BudgetService implements SetBudgetUseCase, ListBudgetStatusUseCase, DeleteBudgetUseCase {

    private final BudgetRepositoryPort budgetRepository;
    private final CategoryRepositoryPort categoryRepository;
    private final ExpenseRepositoryPort expenseRepository;

    public BudgetService(BudgetRepositoryPort budgetRepository,
                         CategoryRepositoryPort categoryRepository,
                         ExpenseRepositoryPort expenseRepository) {
        this.budgetRepository = Objects.requireNonNull(budgetRepository, "O repositório de orçamentos é obrigatório.");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "O repositório de categorias é obrigatório.");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "O repositório de despesas é obrigatório.");
    }

    @Override
    public Budget execute(SetBudgetCommand command) {
        validateCategory(command.categoryId(), command.userId());

        Money limite = new Money(command.monthlyLimit(), command.currency());

        // Idempotente: se já existe limite para esta categoria, altera o valor em vez de criar
        // um segundo. É o que permite o PUT, e o que faz o índice único do banco nunca ser
        // acionado pelo caminho normal — ele fica lá como rede de segurança contra corrida.
        return budgetRepository.findByUserIdAndCategoryId(command.userId(), command.categoryId())
                .map(existente -> {
                    existente.changeLimit(limite);
                    return budgetRepository.save(existente);
                })
                .orElseGet(() -> budgetRepository.save(
                        new Budget(null, command.userId(), command.categoryId(), limite)));
    }

    @Override
    public List<BudgetStatus> execute(UUID userId, YearMonth month) {
        Objects.requireNonNull(month, "O mês é obrigatório.");

        // Sem atalho para "nenhum limite definido": a lista inclui as categorias que tiveram
        // movimento no mês mesmo sem limite, então os lançamentos precisam ser buscados de
        // qualquer forma. É justamente o usuário que ainda não configurou nada que mais
        // precisa ver onde o dinheiro está indo.
        List<Budget> orcamentos = budgetRepository.findByUserId(userId);

        LocalDate primeiroDia = month.atDay(1);
        LocalDate ultimoDia = month.atEndOfMonth();

        List<Expense> pagas = expenseRepository.findByUserIdAndPaidAtBetween(
                userId, primeiroDia.atStartOfDay(), ultimoDia.atTime(LocalTime.MAX));

        List<Expense> aPagar = expenseRepository.findByUserIdAndDueDateUpTo(userId, ultimoDia)
                .stream()
                .filter(despesa -> !despesa.isPaid())
                .toList();

        Map<UUID, String> nomes = categoryRepository.findByUserId(userId, true).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));

        return BudgetStatus.forMonth(orcamentos, pagas, aPagar, nomes);
    }

    @Override
    public void execute(UUID userId, UUID categoryId) {
        Budget orcamento = budgetRepository.findByUserIdAndCategoryId(userId, categoryId)
                .filter(b -> b.isOwnedBy(userId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Nenhum orçamento definido para esta categoria."));

        budgetRepository.deleteById(orcamento.getId());
    }

    /**
     * Mesmas regras da despesa: a categoria precisa existir, ser do usuário e estar ativa.
     *
     * <p>A mensagem é a mesma para "não existe" e "é de outro dono" — categoria de outra pessoa
     * não deve ser distinguível de categoria inexistente, ou o erro vira um jeito de descobrir
     * quais ids existem no sistema.
     */
    private void validateCategory(UUID categoryId, UUID userId) {
        if (categoryId == null) {
            throw new BusinessException("A categoria é obrigatória.");
        }

        Category category = categoryRepository.findById(categoryId)
                .filter(c -> c.isOwnedBy(userId))
                .orElseThrow(() -> new BusinessException("Categoria inválida."));

        if (!category.isActive()) {
            throw new BusinessException(
                    "A categoria \"" + category.getName() + "\" está arquivada e não aceita orçamento.");
        }
    }
}
