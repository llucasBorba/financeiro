package br.com.gastos.financeiro.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record ChangeCategoryStatusRequest(

        @Schema(description = "false arquiva a categoria (some dos seletores, histórico intacto); "
                + "true reativa.", example = "false")
        @NotNull(message = "Informe se a categoria deve ficar ativa.")
        Boolean active
) {}
