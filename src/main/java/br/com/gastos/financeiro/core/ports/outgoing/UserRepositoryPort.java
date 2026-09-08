package br.com.gastos.financeiro.core.ports.outgoing;

import br.com.gastos.financeiro.core.model.Email;
import br.com.gastos.financeiro.core.model.User;

import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(UUID id);

    Optional<User> findByEmail(Email email);

    Optional<User> findByGoogleId(String googleId);
}
