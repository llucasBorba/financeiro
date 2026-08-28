package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase;
import br.com.gastos.financeiro.infrastructure.web.dto.CreateExpenseRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.ExpenseResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.MarkAsPaidRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final CreateExpenseUseCase createExpense;
    private final MarkExpenseAsPaidUseCase markExpenseAsPaid;
    private final FindExpenseUseCase findExpense;

    public ExpenseController(CreateExpenseUseCase createExpense,
                             MarkExpenseAsPaidUseCase markExpenseAsPaid,
                             FindExpenseUseCase findExpense) {
        this.createExpense = createExpense;
        this.markExpenseAsPaid = markExpenseAsPaid;
        this.findExpense = findExpense;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@Valid @RequestBody CreateExpenseRequest request) {
        Expense expense = createExpense.execute(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/expenses/" + expense.getId()))
                .body(ExpenseResponse.from(expense));
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<ExpenseResponse> markAsPaid(@PathVariable UUID id,
                                                      @Valid @RequestBody MarkAsPaidRequest request) {
        Expense expense = markExpenseAsPaid.execute(request.toCommand(id));
        return ResponseEntity.ok(ExpenseResponse.from(expense));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> findById(@PathVariable UUID id,
                                                    @RequestParam @NotNull UUID userId) {
        return ResponseEntity.ok(ExpenseResponse.from(findExpense.findById(id, userId)));
    }

    /**
     * Lista as despesas do usuário. Informando {@code startDate} e {@code endDate}
     * a busca é restrita às despesas com vencimento dentro do período.
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> list(
            @RequestParam @NotNull UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Expense> expenses = (startDate != null && endDate != null)
                ? findExpense.listByUserAndPeriod(userId, startDate, endDate)
                : findExpense.listByUser(userId);

        return ResponseEntity.ok(expenses.stream().map(ExpenseResponse::from).toList());
    }
}
