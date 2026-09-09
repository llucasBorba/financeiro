package br.com.gastos.financeiro.core.ports.ingoing;

import java.util.UUID;

/**
 * Troca a senha de quem já está autenticado.
 *
 * <p>A prova de identidade é a <b>senha atual</b>, não um link por e-mail: quem está logado já
 * provou quem é ao entrar. Por isso este fluxo não depende de envio de e-mail nenhum — é o
 * caminho que existe para quem desconfia que a senha vazou.
 *
 * <p>Exigir a senha atual não é burocracia: sem ela, um token roubado viraria acesso
 * permanente, porque o ladrão trocaria a senha e continuaria entrando depois que o token
 * expirasse — e ainda deixaria o dono de fora.
 */
public interface ChangePasswordUseCase {

    record ChangePasswordCommand(
            UUID userId,
            String currentPassword,
            String newPassword
    ) {}

    void execute(ChangePasswordCommand command);
}
