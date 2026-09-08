package br.com.gastos.financeiro.infrastructure.config.security;

import br.com.gastos.financeiro.core.ports.outgoing.PasswordHasherPort;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Adapter de hash de senha usando BCrypt.
 *
 * <p>BCrypt é <em>propositalmente lento</em>. Um hash rápido (MD5, SHA-256) é péssimo para
 * senhas justamente por ser rápido: uma GPU testa bilhões por segundo. O fator de custo abaixo
 * faz cada tentativa custar dezenas de milissegundos, o que é irrelevante para um login legítimo
 * e inviabiliza força bruta em massa.
 *
 * <p>O BCrypt também embute um <em>salt</em> aleatório no próprio hash: duas pessoas com a mesma
 * senha geram hashes diferentes, e uma tabela pré-computada (rainbow table) não serve para nada.
 */
@Component
public class BCryptPasswordHasher implements PasswordHasherPort {

    /** 2^10 iterações. Padrão do Spring; equilibra segurança e tempo de resposta. */
    private static final int COST = 10;

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(COST);

    /**
     * Hash descartável usado quando não há senha real para comparar.
     *
     * <p>Sem ele, um login com e-mail inexistente responderia em ~1ms e um com e-mail existente
     * em ~50ms. Essa diferença é medível e revela quais e-mails estão cadastrados. Gastando o
     * mesmo tempo nos dois casos, o cronômetro do atacante não conta nada.
     */
    private final String decoyHash = encoder.encode("senha-isca-para-tempo-constante");

    @Override
    public String hash(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        if (rawPassword == null) {
            return false;
        }
        if (hash == null) {
            encoder.matches(rawPassword, decoyHash);   // queima o mesmo tempo de propósito
            return false;
        }
        return encoder.matches(rawPassword, hash);
    }
}
