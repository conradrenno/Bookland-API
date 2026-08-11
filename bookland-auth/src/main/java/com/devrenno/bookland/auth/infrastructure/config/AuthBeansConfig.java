package com.devrenno.bookland.auth.infrastructure.config;

import com.devrenno.bookland.auth.adapters.controller.AuthController;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the auth module. Builds the framework-free inner graph from the outbound
 * ports (implemented by Spring adapters) and exposes the internal AuthController as a bean.
 *
 * <p>One port left, from five. Token issuance, refresh-token persistence and credential checking
 * all moved into the Authorization Server, which needs no port of ours to do them.
 */
@Configuration
public class AuthBeansConfig {

    @Bean
    public AuthController authController(UserRegistrationPort userRegistrationPort) {
        return AuthController.create(userRegistrationPort);
    }
}
