package br.com.gastos.financeiro.infrastructure.web.dto;

import br.com.gastos.financeiro.core.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Nunca inclua {@code passwordHash} aqui. Expor o hash permite quebrar a senha offline,
 * sem limite de tentativas e sem deixar rastro no servidor.
 */
public record UserResponse(
        UUID id,
        String email,
        String name,
        boolean emailVerified,
        boolean hasPassword,
        boolean googleLinked,
        LocalDateTime createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail().value(),
                user.getName(),
                user.isEmailVerified(),
                user.hasPassword(),
                user.getGoogleId() != null,
                user.getCreatedAt());
    }
}
