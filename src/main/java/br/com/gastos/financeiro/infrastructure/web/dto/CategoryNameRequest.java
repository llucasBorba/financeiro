package br.com.gastos.financeiro.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Corpo do POST (criar) e do PUT (renomear) — os dois carregam só o nome.
 * Um record por endpoint seria duplicação idêntica esperando para divergir sem motivo.
 */
public record CategoryNameRequest(

        @Schema(description = "Nome da categoria. Único entre as suas.", example = "Alimentação")
        @NotBlank(message = "O nome da categoria é obrigatório.")
        @Size(max = 60, message = "O nome deve ter no máximo 60 caracteres.")
        String name
) {}
