package com.devrenno.bookland.auth.infrastructure.security;

import org.springframework.security.jackson.SecurityJacksonModules;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import java.util.List;

/**
 * Builds the mapper {@code JdbcOAuth2AuthorizationService} uses for the
 * {@code oauth2_authorization.attributes} column.
 *
 * <p>It is the stock Spring Security setup plus two additions for {@link BooklandUserDetails}: the
 * allowlist entry that lets the type be resolved on the way back, and the mixin that gives it a
 * shape Jackson can write and read. Both are needed — the allowlist alone leaves Jackson with no
 * usable creator, and the mixin alone never gets consulted because resolution is refused first.
 *
 * <p>Note this is Jackson 3 throughout — {@code tools.jackson}, {@code JsonMapper}, and the
 * {@code JsonMapper*} mapper classes. The Authorization Server jar also ships a complete Jackson 2
 * set, and every example written before Boot 4 uses it: {@code ObjectMapper},
 * {@code OAuth2AuthorizationServerJackson2Module}, {@code setObjectMapper}. That path still compiles
 * and still runs, on the wrong mapper.
 */
public final class AuthorizationJsonMapperFactory {

    private AuthorizationJsonMapperFactory() {
    }

    public static JsonMapper create() {
        ClassLoader classLoader = AuthorizationJsonMapperFactory.class.getClassLoader();

        BasicPolymorphicTypeValidator.Builder allowlist = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(BooklandUserDetails.class);

        List<JacksonModule> securityModules = SecurityJacksonModules.getModules(classLoader, allowlist);

        return JsonMapper.builder()
                .addModules(securityModules)
                .addMixIn(BooklandUserDetails.class, BooklandUserDetailsMixin.class)
                .build();
    }
}
