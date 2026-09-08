package br.com.gastos.financeiro.infrastructure.config;

import br.com.gastos.financeiro.core.ports.outgoing.CategoryRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.ExpenseRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.IncomeRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.RecurringExpenseRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.PasswordHasherPort;
import br.com.gastos.financeiro.core.ports.outgoing.UserRepositoryPort;
import br.com.gastos.financeiro.core.ports.outgoing.FinancialGoalRepositoryPort;
import br.com.gastos.financeiro.core.service.CategoryService;
import br.com.gastos.financeiro.core.service.ExpenseService;
import br.com.gastos.financeiro.core.service.IncomeService;
import br.com.gastos.financeiro.core.service.RecurringExpenseService;
import br.com.gastos.financeiro.core.service.SummaryService;
import br.com.gastos.financeiro.core.service.FinancialGoalService;
import br.com.gastos.financeiro.core.service.UserService;
import br.com.gastos.financeiro.infrastructure.transaction.TransactionalCategoryService;
import br.com.gastos.financeiro.infrastructure.transaction.TransactionalExpenseService;
import br.com.gastos.financeiro.infrastructure.transaction.TransactionalIncomeService;
import br.com.gastos.financeiro.infrastructure.transaction.TransactionalRecurringExpenseService;
import br.com.gastos.financeiro.infrastructure.transaction.TransactionalSummaryService;
import br.com.gastos.financeiro.infrastructure.transaction.TransactionalFinancialGoalService;
import br.com.gastos.financeiro.infrastructure.transaction.TransactionalUserService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registra os serviços de domínio como beans a partir da infraestrutura.
 * Assim o pacote {@code core} continua livre de anotações do Spring, como manda
 * a arquitetura hexagonal: o framework conhece o domínio, o domínio não conhece o framework.
 *
 * <p>Repare que o serviço de domínio é construído <em>dentro</em> da definição do bean, e não
 * exposto como um bean próprio: se {@code ExpenseService} e {@code TransactionalExpenseService}
 * fossem ambos beans, os dois implementariam {@code CreateExpenseUseCase} e a injeção nos
 * controllers ficaria ambígua. O único bean que implementa as ports é o decorator transacional.
 */
@Configuration
public class UseCaseConfig {

    @Bean
    public TransactionalExpenseService expenseUseCases(ExpenseRepositoryPort expenseRepositoryPort,
                                                       CategoryRepositoryPort categoryRepositoryPort) {
        return new TransactionalExpenseService(
                new ExpenseService(expenseRepositoryPort, categoryRepositoryPort));
    }

    @Bean
    public TransactionalCategoryService categoryUseCases(CategoryRepositoryPort categoryRepositoryPort,
                                                         ExpenseRepositoryPort expenseRepositoryPort) {
        return new TransactionalCategoryService(
                new CategoryService(categoryRepositoryPort, expenseRepositoryPort));
    }

    @Bean
    public TransactionalFinancialGoalService financialGoalUseCases(FinancialGoalRepositoryPort financialGoalRepositoryPort) {
        return new TransactionalFinancialGoalService(new FinancialGoalService(financialGoalRepositoryPort));
    }

    @Bean
    public TransactionalIncomeService incomeUseCases(IncomeRepositoryPort incomeRepositoryPort) {
        return new TransactionalIncomeService(new IncomeService(incomeRepositoryPort));
    }

    @Bean
    public TransactionalSummaryService summaryUseCases(ExpenseRepositoryPort expenseRepositoryPort,
                                                       IncomeRepositoryPort incomeRepositoryPort,
                                                       CategoryRepositoryPort categoryRepositoryPort) {
        return new TransactionalSummaryService(
                new SummaryService(expenseRepositoryPort, incomeRepositoryPort, categoryRepositoryPort));
    }

    @Bean
    public TransactionalRecurringExpenseService recurringExpenseUseCases(
            RecurringExpenseRepositoryPort recurringExpenseRepositoryPort,
            ExpenseRepositoryPort expenseRepositoryPort,
            CategoryRepositoryPort categoryRepositoryPort) {
        return new TransactionalRecurringExpenseService(new RecurringExpenseService(
                recurringExpenseRepositoryPort, expenseRepositoryPort, categoryRepositoryPort));
    }

    @Bean
    public TransactionalUserService userUseCases(UserRepositoryPort userRepositoryPort,
                                                 PasswordHasherPort passwordHasherPort,
                                                 CategoryRepositoryPort categoryRepositoryPort) {
        return new TransactionalUserService(
                new UserService(userRepositoryPort, passwordHasherPort, categoryRepositoryPort));
    }
}
