package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateUserUseCase.LoginCommand;
import jakarta.validation.constraints.NotBlank;

/**
 * Sem {@code @Email} nem tamanho mínimo de senha aqui, de propósito: no login, uma credencial
 * malformada é apenas uma credencial inválida. Validar formato daria ao atacante uma resposta
 * diferente para "e-mail mal escrito" e "senha errada", e cada resposta distinta é informação.
 */
public record LoginRequest(

        @NotBlank(message = "O e-mail é obrigatório.")
        String email,

        @NotBlank(message = "A senha é obrigatória.")
        String password
) {
    public LoginCommand toCommand() {
        return new LoginCommand(email, password);
    }
}
