package com.devrenno.bookland.user.domain.entity;

import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.Getter;

import java.time.Instant;
import java.util.Objects;

@Getter
public class User {

    private final UserId id;
    private String name;
    private final Email email;
    private String passwordHash;
    private UserRole role;
    private final Instant createdAt;
    private Instant updatedAt;
    private boolean active;

    private User(UserId id, String name, Email email, String passwordHash, UserRole role,
                 Instant createdAt, Instant updatedAt, boolean active) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.active = active;
    }

    /**
     * Creates a brand-new user, enforcing the entity's intrinsic invariants.
     * Cross-aggregate rules (e.g. email uniqueness) are enforced by UserDomainService.
     */
    public static User create(String name, Email email, String passwordHash, UserRole role) {
        validateName(name);
        Objects.requireNonNull(email, "email must not be null");
        validatePasswordHash(passwordHash);
        Instant now = Instant.now();
        return new User(
                UserId.generate(),
                name,
                email,
                passwordHash,
                role != null ? role : UserRole.CUSTOMER,
                now,
                now,
                true
        );
    }

    /**
     * Rehydrates a user from persisted state — no validation, no id/timestamp regeneration.
     * Intended for persistence adapters only.
     */
    public static User reconstitute(UserId id, String name, Email email, String passwordHash, UserRole role,
                                    Instant createdAt, Instant updatedAt, boolean active) {
        return new User(id, name, email, passwordHash, role, createdAt, updatedAt, active);
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }

    private static void validateName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("User name must not be blank");
        }
    }

    private static void validatePasswordHash(String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
    }
}
