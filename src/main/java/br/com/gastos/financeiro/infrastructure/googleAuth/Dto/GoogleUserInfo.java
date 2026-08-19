package br.com.gastos.financeiro.infrastructure.googleAuth.Dto;

public record GoogleUserInfo(
        String googleId,
        String email,
        String name,
        String pictureUrl,
        boolean emailVerified
) {
}
