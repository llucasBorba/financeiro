package br.com.gastos.financeiro.infrastructure.config;

import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.FinancialGoalRepositoryPort;
import br.com.gastos.financeiro.core.service.ExpenseService;
import br.com.gastos.financeiro.core.service.FinancialGoalService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os serviços de domínio como beans a partir da infraestrutura.
 * Assim o pacote {@code core} continua livre de anotações do Spring, como manda
 * a arquitetura hexagonal: o framework conhece o domínio, o domínio não conhece o framework.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public ExpenseService expenseService(ExpenseRepositoryPort expenseRepositoryPort) {
        return new ExpenseService(expenseRepositoryPort);
    }

    @Bean
    public FinancialGoalService financialGoalService(FinancialGoalRepositoryPort financialGoalRepositoryPort) {
        return new FinancialGoalService(financialGoalRepositoryPort);
    }
}
