package com.devrenno.bookland.user.infrastructure.bootstrap;

import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.port.in.GetUserByEmailUseCase;
import com.devrenno.bookland.user.application.port.in.RegisterUserUseCase;
import com.devrenno.bookland.user.domain.entity.UserRole;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.Email;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Guarantees an admin account on every start, in every profile. Idempotent: it looks the e-mail up
 * before creating anything, so a restart against the same database changes nothing.
 *
 * <p>Lives in the user module, not in the application that assembles it, because it touches nothing
 * but this module's use cases: whichever process hosts the user module seeds its admin, and no
 * other process needs to know the user module exists.
 */
@Component
@Order(1)
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserByEmailUseCase getUserByEmailUseCase;
    private final AdminProperties adminProperties;

    public AdminBootstrap(RegisterUserUseCase registerUserUseCase,
                          GetUserByEmailUseCase getUserByEmailUseCase,
                          AdminProperties adminProperties) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserByEmailUseCase = getUserByEmailUseCase;
        this.adminProperties = adminProperties;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            getUserByEmailUseCase.execute(Email.of(adminProperties.email()));
            log.info("[BOOTSTRAP] Admin already exists — skipping creation ({})", adminProperties.email());
        } catch (UserNotFoundException e) {
            registerUserUseCase.execute(new CreateUserCommand(
                    "Admin", adminProperties.email(), adminProperties.password(), UserRole.ADMIN
            ));
            log.info("[BOOTSTRAP] Admin user created — {}", adminProperties.email());
        }
    }
}
