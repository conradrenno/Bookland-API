package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Adapts the module's credential to the contract Spring Security authenticates against.
 *
 * <p>Two classes rather than one, on purpose: {@link AuthUserDto} lives in {@code application/dto}
 * and is framework-free by a rule ArchUnit enforces, so it cannot implement a Spring interface.
 * This is the same port/adapter split as everywhere else in the project — the credential inside,
 * the framework's shape at the edge.
 *
 * <p><strong>Why it carries the user id.</strong> Identity reaches the token as a single string
 * propagated through four steps: {@code getUsername()} to {@code Authentication.getName()} to
 * {@code OAuth2Authorization.principalName} to the {@code sub} claim. Declaring the e-mail as the
 * login identifier therefore puts the e-mail in {@code sub} — which OIDC forbids, since {@code sub}
 * must be stable and opaque while an e-mail is neither, and which the Bookland schema cannot use at
 * all: every business column stores the {@code UserId}.
 *
 * <p>The fix does not belong here. Making {@code getUsername()} return the UUID would break the
 * invariant that {@code loadUserByUsername(x).getUsername().equals(x)}, quietly disabling
 * remember-me, {@code UserDetailsPasswordService} and any audit keyed on {@code getName()}. Instead
 * this class carries the id as a field of its own, and {@link BooklandTokenCustomizer} — the
 * extension point the framework publishes for exactly this — reads it when writing the claims.
 *
 * <p>The account flags stay at their permissive defaults, matching what the outgoing
 * {@code LoginService} did: it never consulted {@code active} either. Wiring {@code isEnabled()} to
 * it would be a behaviour change, and belongs in its own commit.
 */
public final class BooklandUserDetails implements UserDetails {

    private final UUID userId;
    private final String email;
    private final String passwordHash;
    private final UserRole role;

    public BooklandUserDetails(UUID userId, String email, String passwordHash, UserRole role) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.email = Objects.requireNonNull(email, "email");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.role = Objects.requireNonNull(role, "role");
    }

    public static BooklandUserDetails from(AuthUserDto user) {
        return new BooklandUserDetails(user.id(), user.email(), user.passwordHash(), user.role());
    }

    public UUID getUserId() {
        return userId;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
