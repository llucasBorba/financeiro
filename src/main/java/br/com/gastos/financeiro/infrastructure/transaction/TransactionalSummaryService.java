package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.MonthlySummary;
import br.com.gastos.financeiro.core.ports.ingoing.GetMonthlySummaryUseCase;
import br.com.gastos.financeiro.core.service.SummaryService;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * O resumo faz quatro consultas (receitas, pagas, a pagar, categorias). Envolvê-las numa
 * transação de leitura garante que todas enxerguem o mesmo instante do banco — sem isso,
 * uma despesa paga entre a primeira e a segunda consulta apareceria em nenhuma ou nas duas.
 */
public class TransactionalSummaryService implements GetMonthlySummaryUseCase {

    private final SummaryService delegate;

    public TransactionalSummaryService(SummaryService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional(readOnly = true)
    public List<MonthlySummary> execute(UUID userId, YearMonth month) {
        return delegate.execute(userId, month);
    }
}
