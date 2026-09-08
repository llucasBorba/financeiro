package br.com.gastos.financeiro.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * O {@code id_token} que o front recebe do Google (Sign-In / One Tap) e repassa para cá.
 *
 * <p>Substitui o {@code @RequestBody String} do controller anterior, que recebia o corpo cru:
 * qualquer cliente mandando o JSON normal {@code {"idToken": "..."}} quebrava.
 */
public record GoogleLoginRequest(

        @NotBlank(message = "O id_token do Google é obrigatório.")
        String idToken
) {}
