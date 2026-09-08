package br.com.gastos.financeiro.core.ports.outgoing;

import br.com.gastos.financeiro.core.model.Income;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IncomeRepositoryPort {

    Income save(Income income);

    Optional<Income> findById(UUID id);

    List<Income> findByUserId(UUID userId);

    List<Income> findByUserIdAndReceivedAtBetween(UUID userId, LocalDate startDate, LocalDate endDate);

    void deleteById(UUID id);
}
