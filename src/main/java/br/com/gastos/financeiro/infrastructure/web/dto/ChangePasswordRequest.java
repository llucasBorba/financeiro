package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.ChangePasswordUseCase.ChangePasswordCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record ChangePasswordRequest(

        @Schema(description = "A senha em uso hoje. É ela que prova que quem pede a troca é o dono.")
        @NotBlank(message = "A senha atual é obrigatória.")
        String currentPassword,

        @Schema(description = "A nova senha. Precisa ser diferente da atual.", example = "uma-senha-bem-longa")
        @NotBlank(message = "A nova senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String newPassword
) {
    public ChangePasswordCommand toCommand(UUID userId) {
        return new ChangePasswordCommand(userId, currentPassword, newPassword);
    }
}
