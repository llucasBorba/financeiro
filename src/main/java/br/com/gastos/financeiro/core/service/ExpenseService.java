package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.model.enums.ExpenseType;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class ExpenseService implements CreateExpenseUseCase, MarkExpenseAsPaidUseCase, FindExpenseUseCase {

    private final ExpenseRepositoryPort expenseRepository;

    public ExpenseService(ExpenseRepositoryPort expenseRepository) {
        this.expenseRepository = Objects.requireNonNull(expenseRepository, "O repositório é obrigatório.");
    }

    @Override
    public Expense execute(CreateExpenseCommand command) {

        ExpenseType type;
        try {
            type = ExpenseType.valueOf(command.type().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new BusinessException("Tipo de despesa inválido. Use 'FIXED' ou 'VARIABLE'.");
        }

        Money amount = new Money(command.amount(), command.currency());

        Expense expense = new Expense(
                null,
                command.userId(),
                command.categoryId(),
                amount,
                command.description(),
                command.dueDate(),
                type
        );

        return expenseRepository.save(expense);
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
    public Expense findById(UUID expenseId, UUID userId) {
        Expense expense = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada com o ID: " + expenseId));

        // Garante que o usuário que está acessando é o dono da despesa (Segurança de Domínio)
        if (!expense.isOwnedBy(userId)) {
            throw new BusinessException("Acesso negado: a despesa não pertence ao usuário informado.");
        }

        return expense;
    }

    @Override
    public List<Expense> listByUser(UUID userId) {
        return expenseRepository.findByUserId(userId);
    }

    @Override
    public List<Expense> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        return expenseRepository.findByUserIdAndDueDateBetween(userId, startDate, endDate);
    }
}
