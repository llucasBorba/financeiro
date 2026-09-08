package br.com.gastos.financeiro.infrastructure.database.adapter;

import br.com.gastos.financeiro.core.model.Email;
import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.core.ports.outgoing.UserRepositoryPort;
import br.com.gastos.financeiro.infrastructure.database.entity.UserJpaEntity;
import br.com.gastos.financeiro.infrastructure.database.mapper.UserMapper;
import br.com.gastos.financeiro.infrastructure.database.repository.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final SpringDataUserRepository jpaRepository;

    public UserRepositoryAdapter(SpringDataUserRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    /** Ver {@code ExpenseRepositoryAdapter#save} para o motivo de carregar antes de gravar. */
    @Override
    public User save(User user) {
        UserJpaEntity entity = jpaRepository.findById(user.getId())
                .orElseGet(UserJpaEntity::new);

        UserJpaEntity saved = jpaRepository.save(UserMapper.applyTo(user, entity));
        return UserMapper.toDomain(saved);
    }

    @Override
    public Optional<User> findById(UUID id) {
        return jpaRepository.findById(id).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value()).map(UserMapper::toDomain);
    }

    @Override
    public Optional<User> findByGoogleId(String googleId) {
        return jpaRepository.findByGoogleId(googleId).map(UserMapper::toDomain);
    }
}
