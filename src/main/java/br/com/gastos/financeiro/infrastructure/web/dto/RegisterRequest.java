package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.RegisterUserUseCase.RegisterCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        @Email(message = "E-mail inválido.")
        @Size(max = 254, message = "O e-mail deve ter no máximo 254 caracteres.")
        String email,

        // O limite máximo existe para não deixar alguém mandar 1MB de senha e fazer o BCrypt
        // (que é lento de propósito) virar um vetor de negação de serviço.
        @Schema(description = "Mínimo de 8 caracteres.", example = "uma-senha-boa")
        @NotBlank(message = "A senha é obrigatória.")
        @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
        String password,

        @Size(max = 255, message = "O nome deve ter no máximo 255 caracteres.")
        String name
) {
    public RegisterCommand toCommand() {
        return new RegisterCommand(email, password, name);
    }
}
