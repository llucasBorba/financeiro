package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.model.RecurringExpense;
import br.com.gastos.financeiro.core.ports.ingoing.CreateRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.GenerateOccurrencesUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.RecurringExpenseRepositoryPort;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public class RecurringExpenseService implements CreateRecurringExpenseUseCase, UpdateRecurringExpenseUseCase,
        DeleteRecurringExpenseUseCase, GenerateOccurrencesUseCase, FindRecurringExpenseUseCase {

    private final RecurringExpenseRepositoryPort recurringRepository;
    private final ExpenseRepositoryPort expenseRepository;
    private final CategoryRepositoryPort categoryRepository;

    public RecurringExpenseService(RecurringExpenseRepositoryPort recurringRepository,
                                   ExpenseRepositoryPort expenseRepository,
                                   CategoryRepositoryPort categoryRepository) {
        this.recurringRepository = Objects.requireNonNull(recurringRepository, "O repositório de recorrências é obrigatório.");
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "O repositório de despesas é obrigatório.");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "O repositório de categorias é obrigatório.");
    }

    @Override
    public RecurringExpense execute(CreateRecurringExpenseCommand command) {
        validateCategory(command.categoryId(), command.userId());

        RecurringExpense modelo = RecurringExpense.create(
                command.userId(),
                command.categoryId(),
                new Money(command.amount(), command.currency()),
                command.description(),
                command.dayOfMonth(),
                command.startMonth(),
                command.endMonth());

        RecurringExpense salvo = recurringRepository.save(modelo);

        // Já entrega as ocorrências: cadastrar a regra sem gerar nada não resolveria o
        // problema que ela existe para resolver — não redigitar o aluguel todo mês.
        generateThrough(salvo, salvo.defaultHorizon());

        return salvo;
    }

    @Override
    public List<Expense> execute(GenerateOccurrencesCommand command) {
        RecurringExpense modelo = findById(command.recurringExpenseId(), command.userId());
        YearMonth alvo = command.through() != null ? command.through() : modelo.defaultHorizon();

        return generateThrough(modelo, alvo);
    }

    @Override
    public RecurringExpense execute(UpdateRecurringExpenseCommand command) {
        RecurringExpense modelo = findById(command.recurringExpenseId(), command.userId());
        validateCategory(command.categoryId(), command.userId());

        Money novoValor = new Money(command.amount(), command.currency());
        modelo.update(command.categoryId(), novoValor, command.description(),
                command.dayOfMonth(), command.endMonth());

        RecurringExpense salvo = recurringRepository.save(modelo);

        // Propaga só para o que ainda está por vir e não foi pago.
        for (Expense ocorrencia : futurasNaoPagas(salvo.getId())) {
            YearMonth mes = YearMonth.from(ocorrencia.getDueDate());
            ocorrencia.update(salvo.getCategoryId(), salvo.getAmount(),
                    salvo.getDescription(), salvo.dueDateFor(mes));
            expenseRepository.save(ocorrencia);
        }

        return salvo;
    }

    @Override
    public void execute(UUID recurringExpenseId, UUID userId) {
        RecurringExpense modelo = findById(recurringExpenseId, userId);

        // As futuras não pagas somem: você cancelou, não vai pagar.
        futurasNaoPagas(modelo.getId())
                .forEach(ocorrencia -> expenseRepository.deleteById(ocorrencia.getId()));

        // As pagas e as vencidas ficam. A FK está declarada como ON DELETE SET NULL: elas
        // perdem o vínculo e viram lançamentos avulsos — que é o que passam a ser.
        recurringRepository.deleteById(modelo.getId());
    }

    @Override
    public RecurringExpense findById(UUID recurringExpenseId, UUID userId) {
        RecurringExpense modelo = recurringRepository.findById(recurringExpenseId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Recorrência não encontrada com o ID: " + recurringExpenseId));

        // "Não encontrada" em vez de "acesso negado", como no resto do sistema.
        if (!modelo.isOwnedBy(userId)) {
            throw new ResourceNotFoundException("Recorrência não encontrada com o ID: " + recurringExpenseId);
        }

        return modelo;
    }

    @Override
    public List<RecurringExpense> listByUser(UUID userId) {
        return recurringRepository.findByUserId(userId);
    }

    /**
     * Cria as ocorrências que faltam até {@code alvo}.
     *
     * <p>Idempotente: monta o conjunto dos meses que já têm ocorrência e pula esses. Gerar duas
     * vezes não duplica, e uma ocorrência que você apagou de propósito não volta a aparecer
     * — porque a geração só cria mês que nunca teve nada.
     */
    private List<Expense> generateThrough(RecurringExpense modelo, YearMonth alvo) {
        Set<YearMonth> jaGerados = expenseRepository.findByRecurringExpenseId(modelo.getId()).stream()
                .map(e -> YearMonth.from(e.getDueDate()))
                .collect(Collectors.toSet());

        return modelo.monthsThrough(alvo).stream()
                .filter(mes -> !jaGerados.contains(mes))
                .map(mes -> expenseRepository.save(modelo.occurrenceFor(mes)))
                .toList();
    }

    /**
     * Ocorrências que ainda vão vencer e não foram pagas.
     *
     * <p>A data de hoje faz parte da regra: o aluguel de agosto que você não pagou continua
     * valendo R$ 1.500, mesmo que o valor tenha subido em setembro. Alterar o passado seria
     * reescrever uma dívida que já existe.
     */
    private List<Expense> futurasNaoPagas(UUID recurringExpenseId) {
        LocalDate hoje = LocalDate.now();

        return expenseRepository.findByRecurringExpenseId(recurringExpenseId).stream()
                .filter(ocorrencia -> !ocorrencia.isPaid())
                .filter(ocorrencia -> !ocorrencia.getDueDate().isBefore(hoje))
                .toList();
    }

    private void validateCategory(UUID categoryId, UUID userId) {
        Category categoria = categoryRepository.findById(categoryId)
                .filter(c -> c.isOwnedBy(userId))
                .orElseThrow(() -> new BusinessException("Categoria inválida."));

        if (!categoria.isActive()) {
            throw new BusinessException(
                    "A categoria \"" + categoria.getName() + "\" está arquivada e não aceita novos lançamentos.");
        }
    }
}
