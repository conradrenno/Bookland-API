package com.devrenno.bookland.auth.infrastructure.persistence;

import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.domain.entity.RefreshToken;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class RefreshTokenPersistenceAdapter implements RefreshTokenPersistencePort {

    private final RefreshTokenJpaRepository repository;

    @Override
    public RefreshToken save(RefreshToken token) {
        RefreshTokenJpaEntity entity = toEntity(token);
        RefreshTokenJpaEntity saved = repository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<RefreshToken> findByTokenValue(String tokenValue) {
        return repository.findByTokenValue(tokenValue).map(this::toDomain);
    }

    private RefreshTokenJpaEntity toEntity(RefreshToken token) {
        return new RefreshTokenJpaEntity(
                token.getId(),
                token.getTokenValue(),
                token.getUserId(),
                token.getEmail(),
                token.getRole(),
                token.getExpiresAt(),
                token.isRevoked(),
                token.getCreatedAt()
        );
    }

    private RefreshToken toDomain(RefreshTokenJpaEntity entity) {
        return RefreshToken.reconstitute(
                entity.getId(),
                entity.getTokenValue(),
                entity.getUserId(),
                entity.getEmail(),
                entity.getRole(),
                entity.getExpiresAt(),
                entity.isRevoked(),
                entity.getCreatedAt()
        );
    }
}
