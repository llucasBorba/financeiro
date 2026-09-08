package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.Category;
import br.com.gastos.financeiro.core.model.RecurringExpense;
import br.com.gastos.financeiro.core.ports.ingoing.CreateRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindCategoryUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindRecurringExpenseUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.GenerateOccurrencesUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.GenerateOccurrencesUseCase.GenerateOccurrencesCommand;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateRecurringExpenseUseCase;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import br.com.gastos.financeiro.infrastructure.web.dto.CreateRecurringExpenseRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.ExpenseResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.RecurringExpenseResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.UpdateRecurringExpenseRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Modelos de despesa recorrente. Cada modelo gera despesas comuns em {@code /api/expenses} —
 * é lá que elas são pagas, editadas individualmente ou apagadas.
 */
@RestController
@RequestMapping("/api/recurring-expenses")
public class RecurringExpenseController {

    private final CreateRecurringExpenseUseCase createRecurring;
    private final UpdateRecurringExpenseUseCase updateRecurring;
    private final DeleteRecurringExpenseUseCase deleteRecurring;
    private final GenerateOccurrencesUseCase generateOccurrences;
    private final FindRecurringExpenseUseCase findRecurring;
    private final FindCategoryUseCase findCategory;

    public RecurringExpenseController(CreateRecurringExpenseUseCase createRecurring,
                                      UpdateRecurringExpenseUseCase updateRecurring,
                                      DeleteRecurringExpenseUseCase deleteRecurring,
                                      GenerateOccurrencesUseCase generateOccurrences,
                                      FindRecurringExpenseUseCase findRecurring,
                                      FindCategoryUseCase findCategory) {
        this.createRecurring = createRecurring;
        this.updateRecurring = updateRecurring;
        this.deleteRecurring = deleteRecurring;
        this.generateOccurrences = generateOccurrences;
        this.findRecurring = findRecurring;
        this.findCategory = findCategory;
    }

    @Operation(summary = "Cria a recorrência e já gera as ocorrências",
            description = "Com mês final informado, gera a série inteira. Sem ele, gera 12 meses "
                    + "e você estende depois com POST /{id}/generate.")
    @PostMapping
    public ResponseEntity<RecurringExpenseResponse> create(
            @CurrentUser UUID userId,
            @Valid @RequestBody CreateRecurringExpenseRequest request) {

        RecurringExpense modelo = createRecurring.execute(request.toCommand(userId));
        return ResponseEntity
                .created(URI.create("/api/recurring-expenses/" + modelo.getId()))
                .body(toResponse(modelo, userId));
    }

    @Operation(summary = "Altera o modelo e as ocorrências futuras não pagas",
            description = "Ocorrências já pagas e as vencidas ficam intocadas.")
    @PutMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> update(
            @CurrentUser UUID userId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateRecurringExpenseRequest request) {

        RecurringExpense modelo = updateRecurring.execute(request.toCommand(id, userId));
        return ResponseEntity.ok(toResponse(modelo, userId));
    }

    @Operation(summary = "Estende o horizonte da recorrência",
            description = "Idempotente: mês que já tem ocorrência é pulado. Devolve apenas o que foi criado agora.")
    @PostMapping("/{id}/generate")
    public ResponseEntity<List<ExpenseResponse>> generate(
            @CurrentUser UUID userId,
            @PathVariable UUID id,
            @RequestParam(required = false)
            @Schema(description = "Gerar até este mês (yyyy-MM). Omitido, usa o horizonte padrão.",
                    example = "2028-12")
            YearMonth through) {

        Map<UUID, String> nomes = categoryNames(userId);

        return ResponseEntity.ok(
                generateOccurrences.execute(new GenerateOccurrencesCommand(id, userId, through)).stream()
                        .map(e -> ExpenseResponse.from(e, nomes.get(e.getCategoryId())))
                        .toList());
    }

    @Operation(summary = "Apaga o modelo e as ocorrências futuras não pagas",
            description = "O histórico já pago sobrevive como lançamentos avulsos.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        deleteRecurring.execute(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consulta um modelo de recorrência")
    @GetMapping("/{id}")
    public ResponseEntity<RecurringExpenseResponse> findById(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(toResponse(findRecurring.findById(id, userId), userId));
    }

    @Operation(summary = "Lista os modelos de recorrência",
            description = "Só as regras. Os lançamentos que elas geraram estão em GET /api/expenses.")
    @GetMapping
    public ResponseEntity<List<RecurringExpenseResponse>> list(@CurrentUser UUID userId) {
        Map<UUID, String> nomes = categoryNames(userId);

        return ResponseEntity.ok(findRecurring.listByUser(userId).stream()
                .map(modelo -> RecurringExpenseResponse.from(modelo, nomes.get(modelo.getCategoryId())))
                .toList());
    }

    private RecurringExpenseResponse toResponse(RecurringExpense modelo, UUID userId) {
        return RecurringExpenseResponse.from(modelo,
                findCategory.findById(modelo.getCategoryId(), userId).getName());
    }

    /** Ver {@code ExpenseController#categoryNames}: uma consulta para todos, em vez de N+1. */
    private Map<UUID, String> categoryNames(UUID userId) {
        return findCategory.listByUser(userId, true).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }
}
