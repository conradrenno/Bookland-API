package com.devrenno.bookland.auth.infrastructure.security;

import com.devrenno.bookland.user.domain.entity.UserRole;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.util.UUID;

/**
 * Teaches Jackson how to read a {@link BooklandUserDetails} back out of the
 * {@code oauth2_authorization.attributes} column.
 *
 * <p>An authorization is stored with the whole {@code Authentication} inside it, principal included,
 * and it is read back when the authorization code is exchanged. Spring Security serialises that with
 * polymorphic typing behind an allowlist, so a principal type it does not know is refused on the way
 * back in:
 *
 * <pre>
 * Could not resolve type id 'com.devrenno...BooklandUserDetails' as a subtype of Object:
 * Configured PolymorphicTypeValidator denied resolution
 * </pre>
 *
 * <p>Which is a deserialisation failure, so it cannot happen at startup and does not happen at login
 * either — it happens at the token endpoint, on the first code exchange, and reads as a 500 from a
 * server that had been working a second earlier. Anything that replaces the principal with a custom
 * type owes the allowlist an entry.
 *
 * <p>Fields, not getters: {@code UserDetails} exposes {@code getUsername()}/{@code getPassword()}
 * and four account-status flags, which would serialise under names the constructor cannot take back.
 * Reading the four fields directly makes the written form and the creator the same shape.
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
@JsonAutoDetect(
        fieldVisibility = JsonAutoDetect.Visibility.ANY,
        getterVisibility = JsonAutoDetect.Visibility.NONE,
        isGetterVisibility = JsonAutoDetect.Visibility.NONE)
@JsonIgnoreProperties(ignoreUnknown = true)
abstract class BooklandUserDetailsMixin {

    @JsonCreator
    BooklandUserDetailsMixin(@JsonProperty("userId") UUID userId,
                             @JsonProperty("email") String email,
                             @JsonProperty("passwordHash") String passwordHash,
                             @JsonProperty("role") UserRole role,
                             @JsonProperty("enabled") boolean enabled) {
    }
}
