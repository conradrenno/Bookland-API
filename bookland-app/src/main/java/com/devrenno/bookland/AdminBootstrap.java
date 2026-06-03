package com.devrenno.bookland;

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
