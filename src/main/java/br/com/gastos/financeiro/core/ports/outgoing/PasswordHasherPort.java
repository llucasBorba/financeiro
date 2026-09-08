package br.com.gastos.financeiro.core.ports.outgoing;

/**
 * O que o domínio precisa saber sobre senhas — e nada além disso.
 *
 * <p>Manter isto como port evita que BCrypt (ou o que vier depois: Argon2, scrypt) apareça
 * dentro do {@code core}. Trocar de algoritmo passa a ser trocar de adapter.
 */
public interface PasswordHasherPort {

    /** @return o hash a ser persistido. Nunca guarde a senha em claro. */
    String hash(String rawPassword);

    /**
     * Confere a senha contra um hash.
     *
     * <p>Aceita {@code hash} nulo de propósito: quando o e-mail não existe ou a conta é só
     * Google, o serviço ainda assim chama este método para que a resposta demore o mesmo tanto.
     * Sem isso, o tempo de resposta revelaria quais e-mails estão cadastrados.
     */
    boolean matches(String rawPassword, String hash);
}
