package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.Income;
import br.com.gastos.financeiro.core.ports.ingoing.CreateIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindIncomeUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateIncomeUseCase;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import br.com.gastos.financeiro.infrastructure.web.dto.CreateIncomeRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.IncomeResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.UpdateIncomeRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Espelho do {@code ExpenseController}, menos os endpoints de pagamento — receita não tem
 * ciclo "a receber → recebido".
 */
@RestController
@RequestMapping("/api/incomes")
public class IncomeController {

    private final CreateIncomeUseCase createIncome;
    private final UpdateIncomeUseCase updateIncome;
    private final DeleteIncomeUseCase deleteIncome;
    private final FindIncomeUseCase findIncome;

    public IncomeController(CreateIncomeUseCase createIncome,
                            UpdateIncomeUseCase updateIncome,
                            DeleteIncomeUseCase deleteIncome,
                            FindIncomeUseCase findIncome) {
        this.createIncome = createIncome;
        this.updateIncome = updateIncome;
        this.deleteIncome = deleteIncome;
        this.findIncome = findIncome;
    }

    @Operation(summary = "Registra uma receita",
            description = "A descrição é obrigatória: como receita não tem categoria, é ela que identifica o lançamento.")
    @PostMapping
    public ResponseEntity<IncomeResponse> create(@CurrentUser UUID userId,
                                                 @Valid @RequestBody CreateIncomeRequest request) {
        Income income = createIncome.execute(request.toCommand(userId));
        return ResponseEntity
                .created(URI.create("/api/incomes/" + income.getId()))
                .body(IncomeResponse.from(income));
    }

    @Operation(summary = "Substitui os dados de uma receita")
    @PutMapping("/{id}")
    public ResponseEntity<IncomeResponse> update(@CurrentUser UUID userId,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody UpdateIncomeRequest request) {
        return ResponseEntity.ok(IncomeResponse.from(updateIncome.execute(request.toCommand(id, userId))));
    }

    @Operation(summary = "Exclui uma receita")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        deleteIncome.execute(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Consulta uma receita")
    @GetMapping("/{id}")
    public ResponseEntity<IncomeResponse> findById(@CurrentUser UUID userId, @PathVariable UUID id) {
        return ResponseEntity.ok(IncomeResponse.from(findIncome.findById(id, userId)));
    }

    /** Informando {@code startDate} e {@code endDate}, restringe ao período de recebimento. */
    @Operation(summary = "Lista receitas, da mais recente para a mais antiga")
    @GetMapping
    public ResponseEntity<List<IncomeResponse>> list(
            @CurrentUser UUID userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        List<Income> incomes = (startDate != null && endDate != null)
                ? findIncome.listByUserAndPeriod(userId, startDate, endDate)
                : findIncome.listByUser(userId);

        return ResponseEntity.ok(incomes.stream().map(IncomeResponse::from).toList());
    }
}
