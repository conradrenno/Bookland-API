package com.devrenno.bookland.auth.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {

    private final UUID id;
    private final String tokenValue;
    private final UUID userId;
    private final String email;
    private final String role;
    private final Instant expiresAt;
    private boolean revoked;
    private final Instant createdAt;

    private RefreshToken(UUID id, String tokenValue, UUID userId, String email, String role,
                         Instant expiresAt, boolean revoked, Instant createdAt) {
        this.id = id;
        this.tokenValue = tokenValue;
        this.userId = userId;
        this.email = email;
        this.role = role;
        this.expiresAt = expiresAt;
        this.revoked = revoked;
        this.createdAt = createdAt;
    }

    public static RefreshToken create(UUID userId, String email, String role, long expirationMs) {
        Instant now = Instant.now();
        return new RefreshToken(
                UUID.randomUUID(),
                UUID.randomUUID().toString(),
                userId,
                email,
                role,
                now.plusMillis(expirationMs),
                false,
                now
        );
    }

    public static RefreshToken reconstitute(UUID id, String tokenValue, UUID userId, String email,
                                             String role, Instant expiresAt, boolean revoked, Instant createdAt) {
        return new RefreshToken(id, tokenValue, userId, email, role, expiresAt, revoked, createdAt);
    }

    public void revoke() {
        this.revoked = true;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    public boolean isValid() {
        return !revoked && !isExpired();
    }

    public UUID getId() { return id; }
    public String getTokenValue() { return tokenValue; }
    public UUID getUserId() { return userId; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Instant getExpiresAt() { return expiresAt; }
    public boolean isRevoked() { return revoked; }
    public Instant getCreatedAt() { return createdAt; }
}
