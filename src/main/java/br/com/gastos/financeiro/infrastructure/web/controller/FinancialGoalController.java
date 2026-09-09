package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DeleteGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.UpdateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.WithdrawFromGoalUseCase;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import br.com.gastos.financeiro.infrastructure.web.dto.CreateGoalRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.DepositRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.GoalResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.UpdateGoalRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.WithdrawalRequest;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
public class FinancialGoalController {

    private final CreateGoalUseCase createGoal;
    private final UpdateGoalUseCase updateGoal;
    private final DeleteGoalUseCase deleteGoal;
    private final DepositToGoalUseCase depositToGoal;
    private final WithdrawFromGoalUseCase withdrawFromGoal;
    private final FindGoalUseCase findGoal;

    public FinancialGoalController(CreateGoalUseCase createGoal,
                                   UpdateGoalUseCase updateGoal,
                                   DeleteGoalUseCase deleteGoal,
                                   DepositToGoalUseCase depositToGoal,
                                   WithdrawFromGoalUseCase withdrawFromGoal,
                                   FindGoalUseCase findGoal) {
        this.createGoal = createGoal;
        this.updateGoal = updateGoal;
        this.deleteGoal = deleteGoal;
        this.depositToGoal = depositToGoal;
        this.withdrawFromGoal = withdrawFromGoal;
        this.findGoal = findGoal;
    }

    @Operation(summary = "Cria uma meta financeira")
    @PostMapping
    public ResponseEntity<GoalResponse> create(@CurrentUser UUID userId,
                                               @Valid @RequestBody CreateGoalRequest request) {
        FinancialGoal goal = createGoal.execute(request.toCommand(userId));
        return ResponseEntity
                .created(URI.create("/api/goals/" + goal.getId()))
                .body(GoalResponse.from(goal));
    }

    /** Corrige título, valor alvo e prazo. O saldo acumulado não é afetado. */
    @Operation(summary = "Corrige título, valor alvo e prazo da meta",
            description = "Não toca no saldo acumulado: ele é resultado de aportes, não um campo que se digita.")
    @PutMapping("/{id}")
    public ResponseEntity<GoalResponse> update(@CurrentUser UUID userId,
                                               @PathVariable UUID id,
                                               @Valid @RequestBody UpdateGoalRequest request) {
        return ResponseEntity.ok(GoalResponse.from(updateGoal.execute(request.toCommand(id, userId))));
    }

    @Operation(summary = "Exclui a meta")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID id) {
        deleteGoal.execute(id, userId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Registra um aporte na meta",
            description = "O aporte precisa ser na mesma moeda da meta.")
    @PostMapping("/{id}/deposits")
    public ResponseEntity<GoalResponse> deposit(@CurrentUser UUID userId,
                                                @PathVariable UUID id,
                                                @Valid @RequestBody DepositRequest request) {
        FinancialGoal goal = depositToGoal.execute(request.toCommand(id, userId));
        return ResponseEntity.ok(GoalResponse.from(goal));
    }

    @Operation(summary = "Resgata parte do valor guardado na meta",
               description = "Contrapartida do aporte. Recusa com 400 se o resgate passar do saldo guardado.")
    @PostMapping("/{id}/withdrawals")
    public ResponseEntity<GoalResponse> withdraw(@CurrentUser UUID userId,
                                                 @PathVariable UUID id,
                                                 @Valid @RequestBody WithdrawalRequest request) {
        FinancialGoal goal = withdrawFromGoal.execute(request.toCommand(id, userId));
        return ResponseEntity.ok(GoalResponse.from(goal));
    }

    @Operation(summary = "Consulta uma meta")
    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> findById(@CurrentUser UUID userId,
                                                 @PathVariable UUID id) {
        return ResponseEntity.ok(GoalResponse.from(findGoal.findById(id, userId)));
    }

    @Operation(summary = "Lista as metas")
    @GetMapping
    public ResponseEntity<List<GoalResponse>> list(@CurrentUser UUID userId) {
        return ResponseEntity.ok(findGoal.listByUser(userId).stream().map(GoalResponse::from).toList());
    }
}
