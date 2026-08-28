package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindGoalUseCase;
import br.com.gastos.financeiro.infrastructure.web.dto.CreateGoalRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.DepositRequest;
import br.com.gastos.financeiro.infrastructure.web.dto.GoalResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/goals")
public class FinancialGoalController {

    private final CreateGoalUseCase createGoal;
    private final DepositToGoalUseCase depositToGoal;
    private final FindGoalUseCase findGoal;

    public FinancialGoalController(CreateGoalUseCase createGoal,
                                   DepositToGoalUseCase depositToGoal,
                                   FindGoalUseCase findGoal) {
        this.createGoal = createGoal;
        this.depositToGoal = depositToGoal;
        this.findGoal = findGoal;
    }

    @PostMapping
    public ResponseEntity<GoalResponse> create(@Valid @RequestBody CreateGoalRequest request) {
        FinancialGoal goal = createGoal.execute(request.toCommand());
        return ResponseEntity
                .created(URI.create("/api/goals/" + goal.getId()))
                .body(GoalResponse.from(goal));
    }

    @PostMapping("/{id}/deposits")
    public ResponseEntity<GoalResponse> deposit(@PathVariable UUID id,
                                                @Valid @RequestBody DepositRequest request) {
        FinancialGoal goal = depositToGoal.execute(request.toCommand(id));
        return ResponseEntity.ok(GoalResponse.from(goal));
    }

    @GetMapping("/{id}")
    public ResponseEntity<GoalResponse> findById(@PathVariable UUID id,
                                                 @RequestParam @NotNull UUID userId) {
        return ResponseEntity.ok(GoalResponse.from(findGoal.findById(id, userId)));
    }

    @GetMapping
    public ResponseEntity<List<GoalResponse>> list(@RequestParam @NotNull UUID userId) {
        return ResponseEntity.ok(findGoal.listByUser(userId).stream().map(GoalResponse::from).toList());
    }
}
