package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.dto.CreateExpenseCommand;
import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.model.enums.ExpenseType;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;

import java.util.Objects;

public class ExpenseService implements CreateExpenseUseCase, MarkExpenseAsPaidUseCase {

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
        // 1. Buscar a despesa existente
        Expense expense = expenseRepository.findById(command.expenseId())
                .orElseThrow(() -> new ResourceNotFoundException("Despesa não encontrada com o ID: " + command.expenseId()));

        // 2. Garante que o usuário que está alterando é o dono da despesa (Segurança de Domínio)
        if (!expense.getUserId().equals(command.userId())) {
            throw new BusinessException("Acesso negado: a despesa não pertence ao usuário informado.");
        }

        // 3. Aplicar regra de negócio no modelo
        expense.markAsPaid(command.paymentDate());

        // 4. Persistir atualização
        return expenseRepository.save(expense);
    }
}