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
 * the internal UserController (HTTP delivery) and the use cases consumed cross-module.
 */
@Configuration
public class UserBeansConfig {

    // Bean-exposure rule: a use case becomes a @Bean only when it is a public boundary of the
    // module — i.e. consumed cross-module by other Spring components via its port/in interface.
    // Use cases that are internal to this module's own HTTP delivery (getById/update/delete) are
    // NOT beans; they are built inside UserController.create(...) and stay encapsulated there.

    /** Internal controller = HTTP-delivery entry point. Wires getById/update/delete internally. */
    @Bean
    public UserController userController(UserPersistencePort persistencePort) {
        return UserController.create(persistencePort);
    }

    /** Cross-module: consumed by auth (UserRegistrationAdapter) and bookland-app (AdminBootstrap). */
    @Bean
    public RegisterUserUseCase registerUserUseCase(UserPersistencePort persistencePort,
                                                   PasswordEncoderPort passwordEncoderPort) {
        return RegisterUserService.create(new UserDomainService(), persistencePort, passwordEncoderPort);
    }

    /** Cross-module: consumed by auth (UserLookupAdapter) and bookland-app (AdminBootstrap). */
    @Bean
    public GetUserByEmailUseCase getUserByEmailUseCase(UserPersistencePort persistencePort) {
        return GetUserByEmailService.create(persistencePort);
    }

    /** Cross-module: consumed by reviews (CustomerNameAdapter) to label a review with its author. */
    @Bean
    public GetUserByIdUseCase getUserByIdUseCase(UserPersistencePort persistencePort) {
        return GetUserByIdService.create(persistencePort);
    }
}
