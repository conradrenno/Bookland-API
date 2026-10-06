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
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * The sample customer for local development. Split out of the application's {@code DevDataLoader},
 * which keeps seeding the books: the two never depended on each other, and this half belongs to
 * whichever process hosts the user module. Idempotent, like {@link AdminBootstrap}.
 */
@Component
@Profile("dev")
@Order(2)
public class DevCustomerSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevCustomerSeeder.class);

    private static final String CUSTOMER_EMAIL = "joao@bookland.com";

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserByEmailUseCase getUserByEmailUseCase;

    public DevCustomerSeeder(RegisterUserUseCase registerUserUseCase,
                             GetUserByEmailUseCase getUserByEmailUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserByEmailUseCase = getUserByEmailUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            getUserByEmailUseCase.execute(Email.of(CUSTOMER_EMAIL));
            log.info("[DEV] Customer already exists — skipping ({})", CUSTOMER_EMAIL);
        } catch (UserNotFoundException e) {
            registerUserUseCase.execute(new CreateUserCommand(
                    "João Silva", CUSTOMER_EMAIL, "joao1234", UserRole.CUSTOMER));
            log.info("[DEV] Customer seeded — {} (joao1234)", CUSTOMER_EMAIL);
        }
    }
}
