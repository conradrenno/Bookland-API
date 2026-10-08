package com.devrenno.bookland.user.infrastructure.config;

import com.devrenno.bookland.user.adapters.controller.UserController;
import com.devrenno.bookland.user.application.port.in.GetUserByEmailUseCase;
import com.devrenno.bookland.user.application.port.in.GetUserByIdUseCase;
import com.devrenno.bookland.user.application.port.in.RegisterUserUseCase;
import com.devrenno.bookland.user.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.application.service.GetUserByEmailService;
import com.devrenno.bookland.user.application.service.GetUserByIdService;
import com.devrenno.bookland.user.application.service.RegisterUserService;
import com.devrenno.bookland.user.domain.service.UserDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the user module. Builds the framework-free inner graph from the outbound
 * ports (implemented by Spring adapters) and exposes only the entry points as beans:
 * the internal UserController (HTTP delivery) and the use cases consumed outside it — by the auth
 * module, which runs in the same process (the identity service), and by this module's own
 * bootstrap runners.
 */
@Configuration
public class UserBeansConfig {

    // Bean-exposure rule: a use case becomes a @Bean only when another Spring component consumes it
    // through its port/in interface — the auth module's adapters, or this module's AdminBootstrap
    // and DevCustomerSeeder.
    // Use cases that are internal to this module's own HTTP delivery (getById/update/delete) are
    // NOT beans; they are built inside UserController.create(...) and stay encapsulated there.

    /** Internal controller = HTTP-delivery entry point. Wires getById/update/delete internally. */
    @Bean
    public UserController userController(UserPersistencePort persistencePort) {
        return UserController.create(persistencePort);
    }

    /** Consumed by auth (UserRegistrationAdapter, registration) and by AdminBootstrap / DevCustomerSeeder. */
    @Bean
    public RegisterUserUseCase registerUserUseCase(UserPersistencePort persistencePort,
                                                   PasswordEncoderPort passwordEncoderPort) {
        return RegisterUserService.create(new UserDomainService(), persistencePort, passwordEncoderPort);
    }

    /** Consumed by auth (UserLookupAdapter, login) and by AdminBootstrap / DevCustomerSeeder. */
    @Bean
    public GetUserByEmailUseCase getUserByEmailUseCase(UserPersistencePort persistencePort) {
        return GetUserByEmailService.create(persistencePort);
    }

    /**
     * Consumed by auth (UserLookupAdapter): a token refresh looks the account up again by id, so a
     * deactivated account stops getting tokens. No other module asks the user module who a user is —
     * the caller's identity travels in the access token.
     */
    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserPersistencePort persistencePort) {
        return GetUserByIdService.create(persistencePort);
    }
}
