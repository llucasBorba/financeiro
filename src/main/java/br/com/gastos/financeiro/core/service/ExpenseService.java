package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UndoExpensePaymentUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateExpenseUseCase;
import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ExpenseService implements CreateExpenseUseCase, UpdateExpenseUseCase,
        DeleteExpenseUseCase, MarkExpenseAsPaidUseCase, UndoExpensePaymentUseCase, FindExpenseUseCase {

    private final ExpenseRepositoryPort expenseRepository;
    private final CategoryRepositoryPort categoryRepository;

    public ExpenseService(ExpenseRepositoryPort expenseRepository,
                          CategoryRepositoryPort categoryRepository) {
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "O repositório é obrigatório.");
        this.categoryRepository = Objects.requireNonNull(categoryRepository, "O repositório de categorias é obrigatório.");
    }

    @Override
    public Expense execute(CreateExpenseCommand command) {

        Money amount = new Money(command.amount(), command.currency());
        validateCategory(command.categoryId(), command.userId());

        Expense expense = new Expense(
                null,
                command.userId(),
                command.categoryId(),
                amount,
                command.description(),
                command.dueDate()
        );

        // Compra à vista: nasce e já é quitada. Reusa markAsPaid() em vez de duplicar a
        // transição — as regras de pagamento continuam existindo num lugar só.
        if (command.paidAt() != null) {
            expense.markAsPaid(command.paidAt());
        }

        return expenseRepository.save(expense);
    }

    @Override
    public Expense execute(UpdateExpenseCommand command) {
        // findById já valida a posse: se não for do usuário, sai 404 daqui e a edição nem começa.
        Expense expense = findById(command.expenseId(), command.userId());
        validateCategory(command.categoryId(), command.userId());

        expense.update(
                command.categoryId(),
                new Money(command.amount(), command.currency()),
                command.description(),
                command.dueDate());

        return expenseRepository.save(expense);
    }

    @Override
    public void execute(UUID expenseId, UUID userId) {
        // Mesma checagem de posse do update. Apagar a despesa de outro devolve 404,
        // sem revelar que aquele ID existe.
        Expense expense = findById(expenseId, userId);
        expenseRepository.deleteById(expense.getId());
    }

    @Override
    public Expense execute(MarkAsPaidCommand command) {
        // 1. Buscar a despesa existente (já valida a propriedade do usuário)
        Expense expense = findById(command.expenseId(), command.userId());

        // 2. Aplicar regra de negócio no modelo
        expense.markAsPaid(command.paymentDate());

        // 3. Persistir atualização
        return expenseRepository.save(expense);
    }

    @Override
    public Expense execute(UndoPaymentCommand command) {
        Expense expense = findById(command.expenseId(), command.userId());
        expense.undoPayment();
        return expenseRepository.save(expense);
    }

    @Override
    public Expense findById(UUID expenseId, UUID userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada com o ID: " + expenseId));

        // Garante que o usuário que está acessando é o dono da despesa (Segurança de Domínio).
        //
        // Responde "não encontrado", e não "acesso negado", de propósito: um 403 confirmaria que
        // aquele ID existe, transformando a rota num oráculo para descobrir despesas alheias.
        // Para quem não é dono, o recurso simplesmente não existe — é o que GitHub e GitLab fazem
        // com repositórios privados, e é coerente com não revelar quais e-mails estão cadastrados.
        if (!expense.isOwnedBy(userId)) {
            throw new ResourceNotFoundException("Despesa não encontrada com o ID: " + expenseId);
        }

        return expense;
    }

    @Override
    public List<Expense> listByUser(UUID userId) {
        return expenseRepository.findByUserId(userId);
    }

    /**
     * Confere que a categoria informada pode ser usada por este usuário.
     *
     * <p>A chave estrangeira do banco garante apenas que o ID existe. Que a categoria
     * <em>pertença a quem está cadastrando</em> e que <em>esteja ativa</em> são regras de
     * negócio — nenhum banco expressa isso.
     *
     * <p>A categoria é obrigatória: sem ela, o lançamento entra no total do mês mas some
     * da quebra por categoria — um buraco invisível no resumo. Como toda conta nasce com
     * "Outros", sempre há uma escolha válida.
     *
     * <p>A mensagem é a mesma para "não existe" e "é de outro usuário", de propósito:
     * respostas diferentes permitiriam descobrir os IDs de categoria dos outros.
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
                    "A categoria \"" + category.getName() + "\" está arquivada e não aceita novos lançamentos.");
        }
    }


    @Override
    public List<Expense> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        return expenseRepository.findByUserIdAndDueDateBetween(userId, startDate, endDate);
    }

    @Override
    public List<Expense> listByUserAndRecurrence(UUID userId, UUID recurringExpenseId,
                                                 LocalDate startDate, LocalDate endDate) {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }

        // A consulta por recorrência não filtra por dono — a checagem de posse é feita aqui.
        // Sem ela, informar o id da recorrência de outra pessoa exporia as despesas dela.
        return expenseRepository.findByRecurringExpenseId(recurringExpenseId).stream()
                .filter(despesa -> despesa.isOwnedBy(userId))
                .filter(despesa -> startDate == null || !despesa.getDueDate().isBefore(startDate))
                .filter(despesa -> endDate == null || !despesa.getDueDate().isAfter(endDate))
                .toList();
    }
}
