package br.com.gastos.financeiro.infrastructure.database.mapper;

import br.com.gastos.financeiro.core.model.Email;
import br.com.gastos.financeiro.core.model.User;
import br.com.gastos.financeiro.infrastructure.database.entity.UserJpaEntity;

public class UserMapper {

    private UserMapper() {}

    /** Ver {@link ExpenseMapper#applyTo} para o motivo de receber a entidade de destino. */
    public static UserJpaEntity applyTo(User domain, UserJpaEntity entity) {
        if (domain == null) return null;

        entity.setId(domain.getId());
        entity.setEmail(domain.getEmail().value());
        entity.setName(domain.getName());
        entity.setPasswordHash(domain.getPasswordHash());
        entity.setGoogleId(domain.getGoogleId());
        entity.setEmailVerified(domain.isEmailVerified());
        entity.setCreatedAt(domain.getCreatedAt());

        return entity;
    }

    public static User toDomain(UserJpaEntity entity) {
        if (entity == null) return null;

        return User.reconstitute(
                entity.getId(),
                new Email(entity.getEmail()),
                entity.getName(),
                entity.getPasswordHash(),
                entity.getGoogleId(),
                entity.isEmailVerified(),
                entity.getCreatedAt()
        );
    }
}
