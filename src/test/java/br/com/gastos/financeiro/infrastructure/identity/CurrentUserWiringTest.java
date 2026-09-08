package br.com.gastos.financeiro.infrastructure.identity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Trava a propriedade de segurança mais importante do passo de identidade:
 * <strong>sem um {@link CurrentUserProvider}, a aplicação não sobe.</strong>
 *
 * <p>Hoje o único provider é {@link DevHeaderCurrentUserProvider}, anotado com
 * {@code @Profile("dev")} — ou seja, em produção não existe nenhum. Isso é intencional:
 * é preferível um deploy que falha em segundos a um que atende requisições sem saber
 * quem é o usuário. Se alguém remover o {@code @Profile} ou tornar a dependência
 * opcional, este teste quebra e explica o porquê.
 */
class CurrentUserWiringTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner();

    @Test
    @DisplayName("o contexto falha quando não há provider de identidade registrado")
    void failsWithoutProvider() {
        contextRunner
                .withUserConfiguration(CurrentUserWebConfig.class)
                .run(context -> assertThat(context)
                        .hasFailed()
                        .getFailure()
                        .hasMessageContaining("CurrentUserProvider"));
    }

    @Test
    @DisplayName("o contexto sobe quando existe um provider")
    void startsWithProvider() {
        contextRunner
                .withBean(CurrentUserProvider.class, () -> request -> UUID.randomUUID())
                .withUserConfiguration(CurrentUserWebConfig.class)
                .run(context -> assertThat(context)
                        .hasNotFailed()
                        .hasSingleBean(CurrentUserWebConfig.class));
    }
}
