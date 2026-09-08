package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.exception.BusinessException;
import br.com.gastos.financeiro.core.exception.ResourceNotFoundException;
import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.model.Money;
import br.com.gastos.financeiro.core.ports.ingoing.CreateIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateIncomeUseCase;
import br.com.gastos.financeiro.core.ports.outgoing.IncomeRepositoryPort;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

public class IncomeService implements CreateIncomeUseCase, UpdateIncomeUseCase,
        DeleteIncomeUseCase, FindIncomeUseCase {

    private final IncomeRepositoryPort incomeRepository;

    public IncomeService(IncomeRepositoryPort incomeRepository) {
        this.incomeRepository = Objects.requireNonNull(incomeRepository, "O repositório de receitas é obrigatório.");
    }

    @Override
    public Income execute(CreateIncomeCommand command) {
        Money amount = new Money(command.amount(), command.currency());

        return incomeRepository.save(
                Income.create(command.userId(), amount, command.description(), command.receivedAt()));
    }

    @Override
    public Income execute(UpdateIncomeCommand command) {
        // findById já valida a posse: receita de outro usuário sai como 404 daqui.
        Income income = findById(command.incomeId(), command.userId());

        income.update(new Money(command.amount(), command.currency()),
                command.description(), command.receivedAt());

        return incomeRepository.save(income);
    }

    @Override
    public void execute(UUID incomeId, UUID userId) {
        Income income = findById(incomeId, userId);
        incomeRepository.deleteById(income.getId());
    }

    @Override
    public Income findById(UUID incomeId, UUID userId) {
        Income income = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new ResourceNotFoundException("Receita não encontrada com o ID: " + incomeId));

        // "Não encontrada" em vez de "acesso negado", como nas despesas e categorias:
        // um 403 confirmaria que aquele ID existe.
        if (!income.isOwnedBy(userId)) {
            throw new ResourceNotFoundException("Receita não encontrada com o ID: " + incomeId);
        }

        return income;
    }

    @Override
    public List<Income> listByUser(UUID userId) {
        return incomeRepository.findByUserId(userId);
    }

    @Override
    public List<Income> listByUserAndPeriod(UUID userId, LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data inicial não pode ser posterior à data final.");
        }
        return incomeRepository.findByUserIdAndReceivedAtBetween(userId, startDate, endDate);
    }
}
