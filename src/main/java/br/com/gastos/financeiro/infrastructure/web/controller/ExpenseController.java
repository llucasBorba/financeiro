package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.Expense;
import br.com.gastos.financeiro.core.ports.ingoing.CreateExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.MarkExpenseAsPaidUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UndoExpensePaymentUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UndoExpensePaymentUseCase.UndoPaymentCommand;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateExpenseUseCase;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import io.swagger.v3.oas.annotations.media.Schema;
import br.com.gastos.financeiro.infrastructure.web.dto.CreateExpenseRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.ExpenseResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.MarkAsPaidRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.UpdateExpenseRequest;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.UUID;

@RestController
@RequestMapping("/api/expenses")
public class ExpenseController {

    private final CreateExpenseUseCase createExpense;
    private final UpdateExpenseUseCase updateExpense;
    private final DeleteExpenseUseCase deleteExpense;
    private final MarkExpenseAsPaidUseCase markExpenseAsPaid;
    private final UndoExpensePaymentUseCase undoExpensePayment;
    private final FindExpenseUseCase findExpense;
    private final FindCategoryUseCase findCategory;

    public ExpenseController(CreateExpenseUseCase createExpense,
                             UpdateExpenseUseCase updateExpense,
                             DeleteExpenseUseCase deleteExpense,
                             MarkExpenseAsPaidUseCase markExpenseAsPaid,
                             UndoExpensePaymentUseCase undoExpensePayment,
                             FindExpenseUseCase findExpense,
                             FindCategoryUseCase findCategory) {
        this.createExpense = createExpense;
        this.updateExpense = updateExpense;
        this.deleteExpense = deleteExpense;
        this.markExpenseAsPaid = markExpenseAsPaid;
        this.undoExpensePayment = undoExpensePayment;
        this.findExpense = findExpense;
        this.findCategory = findCategory;
    }

    @PostMapping
    public ResponseEntity<ExpenseResponse> create(@CurrentUser UUID userId,
                                                  @Valid @RequestBody CreateExpenseRequest request) {
        Expense expense = createExpense.execute(request.toCommand(userId));
        return ResponseEntity
                .created(URI.create("/api/expenses/" + expense.getId()))
                .body(toResponse(expense, userId));
    }

    /** Substitui os dados editáveis. O estado de pagamento não é afetado. */
    @PutMapping("/{id}")
    public ResponseEntity<ExpenseResponse> update(@CurrentUser UUID userId,
                                                  @PathVariable UUID id,
                                                  @Valid @RequestBody UpdateExpenseRequest request) {
        Expense expense = updateExpense.execute(request.toCommand(id, userId));
        return ResponseEntity.ok(toResponse(expense, userId));
    }

    /**
     * 204 No Content: a operação deu certo e não há corpo para devolver.
     * Apagar despesa de outro usuário responde 404, como qualquer acesso indevido.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        deleteExpense.execute(id, userId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/payment")
    public ResponseEntity<ExpenseResponse> markAsPaid(@CurrentUser UUID userId,
                                                      @PathVariable UUID id,
                                                      @Valid @RequestBody MarkAsPaidRequest request) {
        Expense expense = markExpenseAsPaid.execute(request.toCommand(id, userId));
        return ResponseEntity.ok(toResponse(expense, userId));
    }

    /**
     * Desfaz um pagamento marcado por engano.
     *
     * <p>{@code DELETE} sobre o sub-recurso "payment": o que se remove é o pagamento, não a
     * despesa. Devolve a despesa atualizada em vez de 204, para o cliente ver o estado novo.
     */
    @DeleteMapping("/{id}/payment")
    public ResponseEntity<ExpenseResponse> undoPayment(@CurrentUser UUID userId,
                                                       @PathVariable UUID id) {
        Expense expense = undoExpensePayment.execute(new UndoPaymentCommand(id, userId));
        return ResponseEntity.ok(toResponse(expense, userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseResponse> findById(@CurrentUser UUID userId,
                                                    @PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findExpense.findById(id, userId), userId));
    }

    /**
     * Lista as despesas do usuário — avulsas e geradas por recorrência, juntas.
     *
     * <p>A listagem não separa por origem de propósito: quem pergunta "o que devo em março?"
     * quer a lista do mês, não duas listas para mesclar. Quando a origem importar, use
     * {@code recurringExpenseId} para restringir a uma regra específica.
     */
    @GetMapping
    public ResponseEntity<List<ExpenseResponse>> list(
            @CurrentUser UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false)
            @Schema(description = "Restringe às ocorrências geradas por esta recorrência "
                    + "(GET /api/recurring-expenses). Combina com startDate e endDate.")
            UUID recurringExpenseId) {

        List<Expense> expenses;
        if (recurringExpenseId != null) {
            expenses = findExpense.listByUserAndRecurrence(userId, recurringExpenseId, startDate, endDate);
        } else if (startDate != null && endDate != null) {
            expenses = findExpense.listByUserAndPeriod(userId, startDate, endDate);
        } else {
            expenses = findExpense.listByUser(userId);
        }

        // Uma consulta só para os nomes, em vez de uma por despesa (N+1). São ~8 categorias
        // por usuário, então carregar todas e cruzar em memória é mais barato que qualquer join.
        Map<UUID, String> nomes = categoryNames(userId);

        return ResponseEntity.ok(expenses.stream()
                .map(e -> ExpenseResponse.from(e, nomes.get(e.getCategoryId())))
                .toList());
    }

    /** Resolve o nome de uma categoria só — para os endpoints que devolvem uma despesa. */
    private ExpenseResponse toResponse(Expense expense, UUID userId) {
        return ExpenseResponse.from(expense, findCategory.findById(expense.getCategoryId(), userId).getName());
    }

    /**
     * Mapa id → nome de TODAS as categorias do usuário, inclusive as arquivadas: uma despesa
     * antiga pode apontar para uma categoria que já foi arquivada, e o nome dela ainda precisa
     * aparecer no histórico.
     */
    private Map<UUID, String> categoryNames(UUID userId) {
        return findCategory.listByUser(userId, true).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }
}
