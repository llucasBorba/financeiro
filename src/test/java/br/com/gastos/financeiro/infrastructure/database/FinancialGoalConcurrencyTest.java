package br.com.gastos.financeiro.infrastructure.database;

import br.com.gastos.financeiro.core.model.FinancialGoal;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.CreateGoalUseCase.CreateGoalCommand;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.DepositToGoalUseCase.DepositCommand;
import br.com.gastos.financeiro.core.ports.ingoing.RegisterUserUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RegisterUserUseCase.RegisterCommand;
import br.com.gastos.financeiro.infrastructure.database.entity.FinancialGoalJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataFinancialGoalRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Prova que o bloqueio otimista impede o "lost update" no aporte.
 *
 * <p>O cenário real são duas requisições simultâneas: as duas leem o saldo 0, as duas somam
 * R$ 100 e a segunda sobrescreve a primeira — R$ 100 somem sem erro nenhum.
 *
 * <p>Reproduzir isso com threads de verdade daria um teste instável (depende de quem ganha a
 * corrida e de timeout de lock do banco). Em vez disso, o teste encena a mesma sequência de
 * forma determinística: guarda a leitura de uma das requisições, deixa a outra terminar, e só
 * então tenta gravar com a leitura antiga. É exatamente o estado em que a primeira requisição
 * estaria no instante da corrida.
 */
@SpringBootTest
class FinancialGoalConcurrencyTest {

    @Autowired
    private CreateGoalUseCase createGoal;

    @Autowired
    private DepositToGoalUseCase depositToGoal;

    @Autowired
    private SpringDataFinancialGoalRepository jpaRepository;

    @Autowired
    private RegisterUserUseCase registerUser;

    /**
     * Cria um usuário de verdade em vez de inventar um UUID.
     *
     * <p>Antes da FK para {@code tb_users}, este teste gravava metas de um dono inexistente e
     * ninguém notava — que é exatamente o tipo de linha órfã que a chave estrangeira passou a
     * impedir. O teste quebrou assim que a restrição entrou: foi ele a primeira prova de que
     * ela funciona.
     */
    private UUID novoUsuario() {
        return registerUser.execute(new RegisterCommand(
                "concorrencia-" + UUID.randomUUID() + "@teste.com", "senha-forte-123", "Teste")).getId();
    }

    @Test
    @DisplayName("recusa a gravação de quem leu a meta antes do aporte de outro")
    void rejectsStaleWrite() {
        UUID userId = novoUsuario();
        FinancialGoal meta = createGoal.execute(new CreateGoalCommand(
                userId, "Reserva de emergência", new BigDecimal("1000.00"), "BRL", LocalDate.of(2027, 1, 1)));

        // Requisição A leu a linha e está com ela em mãos (versão 0, saldo zerado).
        FinancialGoalJpaEntity leituraDeA = jpaRepository.findById(meta.getId()).orElseThrow();
        assertEquals(0L, leituraDeA.getVersion().longValue());

        // Requisição B termina primeiro: o Hibernate incrementa a versão no banco.
        depositToGoal.execute(new DepositCommand(meta.getId(), userId, new BigDecimal("100.00"), "BRL"));
        assertEquals(1L, jpaRepository.findById(meta.getId()).orElseThrow().getVersion().longValue());

        // Agora A tenta gravar em cima de um saldo que já não é o atual.
        leituraDeA.setCurrentAmount(new BigDecimal("999.00"));

        // Antes do @Version isso passaria e apagaria o aporte de B. Agora estoura.
        assertThrows(OptimisticLockingFailureException.class, () -> jpaRepository.save(leituraDeA));

        // E o aporte de B continua intacto — a gravação de A foi recusada, não aplicada pela metade.
        assertEquals(0, new BigDecimal("100.00").compareTo(
                jpaRepository.findById(meta.getId()).orElseThrow().getCurrentAmount()));
    }

    @Test
    @DisplayName("incrementa a versão a cada aporte bem-sucedido")
    void versionAdvancesOnEachWrite() {
        UUID userId = novoUsuario();
        FinancialGoal meta = createGoal.execute(new CreateGoalCommand(
                userId, "Viagem", new BigDecimal("500.00"), "BRL", null));

        depositToGoal.execute(new DepositCommand(meta.getId(), userId, new BigDecimal("100.00"), "BRL"));
        depositToGoal.execute(new DepositCommand(meta.getId(), userId, new BigDecimal("50.00"), "BRL"));

        FinancialGoalJpaEntity persistida = jpaRepository.findById(meta.getId()).orElseThrow();
        assertEquals(2L, persistida.getVersion().longValue());
        assertEquals(0, new BigDecimal("150.00").compareTo(persistida.getCurrentAmount()));
    }
}
