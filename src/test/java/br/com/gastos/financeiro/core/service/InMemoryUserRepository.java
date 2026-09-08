package br.com.gastos.financeiro.core.service;

import br.com.gastos.financeiro.core.model.Email;
import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.core.ports.outgoing.UserRepositoryPort;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Adapter de teste para {@link UserRepositoryPort}: um HashMap no lugar do Postgres. */
public class InMemoryUserRepository implements UserRepositoryPort {

    private final Map<UUID, User> storage = new LinkedHashMap<>();

    @Override
    public User save(User user) {
        storage.put(user.getId(), user);
        return user;
    }

    @Override
    public Optional<User> findById(UUID id) {
        return Optional.ofNullable(storage.get(id));
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return storage.values().stream().filter(u -> u.getEmail().equals(email)).findFirst();
    }

    @Override
    public Optional<User> findByGoogleId(String googleId) {
        return storage.values().stream()
                .filter(u -> googleId.equals(u.getGoogleId()))
                .findFirst();
    }
}
