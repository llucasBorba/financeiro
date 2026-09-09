package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.ports.ingoing.DeleteBudgetUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.ListBudgetStatusUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.SetBudgetUseCase;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import br.com.gastos.financeiro.infrastructure.web.dto.BudgetResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.BudgetStatusResponse;
import br.com.gastos.financeiro.infrastructure.web.dto.SetBudgetRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

/**
 * Limites mensais de gasto por categoria.
 *
 * <p>O recurso é endereçado pela CATEGORIA, não por um id próprio, porque existe no máximo um
 * limite por categoria. Isso torna a gravação idempotente e dispensa o cliente de guardar mais
 * um identificador: quem já tem o id da categoria consegue definir, consultar e apagar o
 * orçamento dela.
 */
@RestController
@RequestMapping("/api/budgets")
public class BudgetController {

    private final SetBudgetUseCase setBudget;
    private final ListBudgetStatusUseCase listBudgetStatus;
    private final DeleteBudgetUseCase deleteBudget;

    public BudgetController(SetBudgetUseCase setBudget,
                            ListBudgetStatusUseCase listBudgetStatus,
                            DeleteBudgetUseCase deleteBudget) {
        this.setBudget = setBudget;
        this.listBudgetStatus = listBudgetStatus;
        this.deleteBudget = deleteBudget;
    }

    @Operation(summary = "Define o limite mensal de uma categoria",
            description = "O limite vale para TODOS os meses, não só para o atual. "
                    + "Idempotente: repetir a requisição atualiza o valor, não cria um segundo limite.")
    @PutMapping("/{categoryId}")
    public ResponseEntity<BudgetResponse> set(@CurrentUser UUID userId,
                                              @PathVariable UUID categoryId,
                                              @Valid @RequestBody SetBudgetRequest request) {
        return ResponseEntity.ok(BudgetResponse.from(setBudget.execute(request.toCommand(userId, categoryId))));
    }

    @Operation(summary = "Lista os limites confrontados com um mês",
            description = "Para cada categoria com limite: quanto foi PAGO no mês, quanto está "
                    + "em aberto vencendo até o fim do mês, e se estourou ou vai estourar. "
                    + "Ordena do mais apertado para o mais folgado. "
                    + "Categoria sem limite definido não aparece.")
    @GetMapping
    public ResponseEntity<List<BudgetStatusResponse>> list(
            @CurrentUser UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            @Schema(description = "Mês no formato yyyy-MM. Omitido, usa o mês corrente.", example = "2026-09")
            YearMonth month) {

        YearMonth alvo = month != null ? month : YearMonth.now();

        return ResponseEntity.ok(listBudgetStatus.execute(userId, alvo).stream()
                .map(BudgetStatusResponse::from)
                .toList());
    }

    @Operation(summary = "Remove o limite de uma categoria",
            description = "A categoria e suas despesas continuam existindo; só o limite deixa de valer.")
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@CurrentUser UUID userId, @PathVariable UUID categoryId) {
        deleteBudget.execute(userId, categoryId);
        return ResponseEntity.noContent().build();
    }
}
