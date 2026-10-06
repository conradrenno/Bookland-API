package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.springframework.security.core.CredentialsContainer;
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
 * <p>{@code enabled} is the account's {@code active} flag. {@code DaoAuthenticationProvider} reads it
 * at login and refuses a deleted account. It is serialised with the rest, but nothing reads it back:
 * a refresh does not trust this snapshot, {@link BooklandTokenCustomizer} looks the account up again.
 *
 * <p>{@code name} is carried for the same customizer, which writes it as the {@code name} claim. It is
 * nullable for the reason {@code passwordHash} is: an authorization stored before the field existed
 * is read back without it.
 */
public final class BooklandUserDetails implements UserDetails, CredentialsContainer {

    private final UUID userId;
    private final String email;
    private final String name;
    /** Not final, and null once {@link #eraseCredentials()} has run. */
    private String passwordHash;
    private final UserRole role;
    private final boolean enabled;

    /**
     * {@code passwordHash} is deliberately not null-checked: it is null on every instance rebuilt
     * from a stored authorization, because it was erased before being written there.
     */
    public BooklandUserDetails(UUID userId, String email, String name, String passwordHash, UserRole role,
                               boolean enabled) {
        this.userId = Objects.requireNonNull(userId, "userId");
        this.email = Objects.requireNonNull(email, "email");
        this.name = name;
        this.passwordHash = passwordHash;
        this.role = Objects.requireNonNull(role, "role");
        this.enabled = enabled;
    }

    /**
     * Drops the password hash once authentication is over.
     *
     * <p>{@code ProviderManager} calls this after a successful authentication, but only on a
     * principal that implements {@link CredentialsContainer} — Spring's own {@code User} does, which
     * is why nobody notices the contract until they write their own. Without it the hash rides along
     * into {@code oauth2_authorization.attributes} and is stored, verified against a real PostgreSQL:
     *
     * <pre>
     * java.security.Principal.principal.passwordHash = '$2a$10$qT3sIpnWje5vcSAl...'
     * </pre>
     *
     * <p>A BCrypt hash in the same database that already holds {@code users.password_hash} is not a
     * breach. It is a credential copied into a second table with a different lifetime, reached by
     * code that has no reason to handle credentials, and carried into every backup of it — for no
     * benefit at all, since nothing re-checks a password from here.
     */
    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }

    public static BooklandUserDetails from(AuthUserDto user) {
        return new BooklandUserDetails(user.id(), user.email(), user.name(), user.passwordHash(), user.role(), user.active());
    }

    public UUID getUserId() {
        return userId;
    }

    /** May be null — see the class comment. */
    public String getName() {
        return name;
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

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
