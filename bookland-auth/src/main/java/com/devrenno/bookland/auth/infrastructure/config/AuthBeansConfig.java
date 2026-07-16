package com.devrenno.bookland.auth.infrastructure.config;

import com.devrenno.bookland.auth.adapters.controller.AuthController;
import com.devrenno.bookland.auth.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.auth.application.port.out.RefreshTokenPersistencePort;
import com.devrenno.bookland.auth.application.port.out.TokenProviderPort;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the auth module. Builds the framework-free inner graph from the outbound
 * ports (implemented by Spring adapters) plus the refresh-token TTL read from JwtProperties,
 * and exposes the internal AuthController (the module's single HTTP-delivery entry point) as a bean.
 */
@Configuration
public class AuthBeansConfig {

    @Bean
    public AuthController authController(UserLookupPort userLookupPort,
                                        UserRegistrationPort userRegistrationPort,
                                        TokenProviderPort tokenProviderPort,
                                        RefreshTokenPersistencePort refreshTokenPersistencePort,
                                        PasswordEncoderPort passwordEncoderPort,
                                        JwtProperties jwtProperties) {
        return AuthController.create(
                userLookupPort,
                userRegistrationPort,
                tokenProviderPort,
                refreshTokenPersistencePort,
                passwordEncoderPort,
                jwtProperties.getRefreshTokenExpirationMs()
        );
    }
}
