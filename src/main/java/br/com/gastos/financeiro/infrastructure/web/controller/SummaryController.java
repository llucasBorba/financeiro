package br.com.gastos.financeiro.infrastructure.web.controller;

import br.com.gastos.financeiro.core.ports.ingoing.GetMonthlySummaryUseCase;
import br.com.gastos.financeiro.infrastructure.identity.CurrentUser;
import br.com.gastos.financeiro.infrastructure.web.dto.MonthlySummaryResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.YearMonth;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/summary")
public class SummaryController {

    private final GetMonthlySummaryUseCase getMonthlySummary;

    public SummaryController(GetMonthlySummaryUseCase getMonthlySummary) {
        this.getMonthlySummary = getMonthlySummary;
    }

    /**
     * Devolve uma lista porque há um fechamento por moeda; com uma moeda só, um item.
     * O resumo é calculado na hora a partir dos lançamentos — qualquer mês passado continua
     * disponível, e sempre reflete o estado atual dos dados.
     */
    @Operation(summary = "Fechamento do mês",
            description = "Entrou = receitas recebidas no mês. Saiu = despesas PAGAS no mês. "
                    + "A pagar = despesas que vencem no mês e seguem pendentes (fora do saldo).")
    @GetMapping("/monthly")
    public ResponseEntity<List<MonthlySummaryResponse>> monthly(
            @CurrentUser UUID userId,
            @RequestParam(required = false)
            @DateTimeFormat(pattern = "yyyy-MM")
            @Schema(description = "Mês no formato yyyy-MM. Omitido, usa o mês corrente.", example = "2026-09")
            YearMonth month) {

        YearMonth alvo = month != null ? month : YearMonth.now();

        return ResponseEntity.ok(getMonthlySummary.execute(userId, alvo).stream()
                .map(MonthlySummaryResponse::from)
                .toList());
    }
}
