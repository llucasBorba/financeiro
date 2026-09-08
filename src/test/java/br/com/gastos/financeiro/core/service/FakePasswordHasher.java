package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.ports.outgoing.PasswordHasherPort;

/**
 * Hasher de teste: reversível e instantâneo.
 *
 * <p>Trocar o BCrypt por isto nos testes de domínio economiza ~50ms por chamada e mantém a
 * suíte do núcleo rodando em milissegundos. O BCrypt de verdade continua sendo exercitado
 * pelos testes de integração, que passam pelo endpoint real de cadastro.
 */
public class FakePasswordHasher implements PasswordHasherPort {

    private static final String PREFIX = "fake-hash:";

    /** Conta quantas comparações foram feitas — usado para provar o gasto de tempo constante. */
    private int matchCalls = 0;

    @Override
    public String hash(String rawPassword) {
        return PREFIX + rawPassword;
    }

    @Override
    public boolean matches(String rawPassword, String hash) {
        matchCalls++;
        return hash != null && hash.equals(PREFIX + rawPassword);
    }

    public int matchCalls() {
        return matchCalls;
    }
}
