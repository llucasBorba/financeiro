package br.com.gastos.financeiro.infrastructure.transaction;

import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateUserUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.AuthenticateWithGoogleUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.FindUserUseCase;
import br.com.gastos.financeiro.core.ports.ingoing.RegisterUserUseCase;
import br.com.gastos.financeiro.core.service.UserService;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Decorator transacional dos casos de uso de usuário.
 * Ver {@link TransactionalExpenseService} para o raciocínio por trás desta camada.
 *
 * <p>Cadastro e login com Google fazem leitura seguida de gravação — o mesmo padrão que causava
 * o "lost update" nos aportes. Aqui o risco é criar duas contas para a mesma pessoa quando dois
 * logins chegam juntos. A transação fecha a janela; o índice UNIQUE no banco é a rede de segurança.
 */
public class TransactionalUserService implements RegisterUserUseCase, AuthenticateUserUseCase,
        AuthenticateWithGoogleUseCase, FindUserUseCase {

    private final UserService delegate;

    public TransactionalUserService(UserService delegate) {
        this.delegate = delegate;
    }

    @Override
    @Transactional
    public User execute(RegisterCommand command) {
        return delegate.execute(command);
    }

    /** Login por senha não grava nada — só confere. */
    @Override
    @Transactional(readOnly = true)
    public User execute(LoginCommand command) {
        return delegate.execute(command);
    }

    /** Este pode criar ou vincular conta, então precisa de transação de escrita. */
    @Override
    @Transactional
    public User execute(GoogleLoginCommand command) {
        return delegate.execute(command);
    }

    @Override
    @Transactional(readOnly = true)
    public User findById(UUID userId) {
        return delegate.findById(userId);
    }
}
