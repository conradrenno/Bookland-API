package com.devrenno.bookland;

import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.port.in.CreateBookUseCase;
import com.devrenno.bookland.catalog.domain.exception.IsbnAlreadyExistsException;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Component
@Profile("dev")
@Order(2)
public class DevDataLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDataLoader.class);

    // Category UUIDs defined in import.sql
    private static final UUID CAT_TECNOLOGIA = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID CAT_FICCAO     = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID CAT_NEGOCIOS   = UUID.fromString("e5f6a7b8-c9d0-1234-efab-345678901234");

    private static final String CUSTOMER_EMAIL = "joao@bookland.com";
    private static final int TOTAL_BOOKS = 5;

    private final RegisterUserUseCase registerUserUseCase;
    private final GetUserByEmailUseCase getUserByEmailUseCase;
    private final CreateBookUseCase createBookUseCase;

    public DevDataLoader(RegisterUserUseCase registerUserUseCase,
                         GetUserByEmailUseCase getUserByEmailUseCase,
                         CreateBookUseCase createBookUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.getUserByEmailUseCase = getUserByEmailUseCase;
        this.createBookUseCase = createBookUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        try {
            getUserByEmailUseCase.execute(Email.of(CUSTOMER_EMAIL));
            log.info("[DEV] Customer already exists — skipping ({})", CUSTOMER_EMAIL);
        } catch (UserNotFoundException e) {
            registerUserUseCase.execute(new CreateUserCommand(
                    "João Silva", CUSTOMER_EMAIL, "joao1234", UserRole.CUSTOMER));
            log.info("[DEV] Users seeded — {} (joao1234)", CUSTOMER_EMAIL);
        }
    }

    private void seedBooks() {
        int created = 0;

        created += seedBook(new CreateBookCommand(
                "Clean Code",
                "978-0132350884",
                List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1ª edição",
                "Um guia de boas práticas para escrita de código limpo e sustentável.",
                BigDecimal.valueOf(44.90), 20, CAT_TECNOLOGIA,
                "https://covers.openlibrary.org/b/isbn/9780132350884-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "The Pragmatic Programmer",
                "978-0135957059",
                List.of("David Thomas", "Andrew Hunt"),
                "Addison-Wesley", 2019, "20th Anniversary Edition",
                "Seu caminho para se tornar um programador pragmático e eficaz.",
                BigDecimal.valueOf(59.90), 15, CAT_TECNOLOGIA,
                "https://covers.openlibrary.org/b/isbn/9780135957059-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "Design Patterns",
                "978-0201633610",
                List.of("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"),
                "Addison-Wesley", 1994, "1ª edição",
                "Os 23 padrões de projeto que todo desenvolvedor precisa conhecer.",
                BigDecimal.valueOf(79.90), 8, CAT_TECNOLOGIA,
                "https://covers.openlibrary.org/b/isbn/9780201633610-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "Duna",
                "978-8576574675",
                List.of("Frank Herbert"),
                "Aleph", 1965, "Edição especial",
                "A épica saga de sobrevivência e poder no planeta deserto Arrakis.",
                BigDecimal.valueOf(49.90), 12, CAT_FICCAO,
                "https://covers.openlibrary.org/b/isbn/9788576574675-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "O Investidor Inteligente",
                "978-8576840220",
                List.of("Benjamin Graham"),
                "HarperCollins", 1949, "Ed. revisada",
                "O guia definitivo de investimento em valor de Benjamin Graham.",
                BigDecimal.valueOf(54.90), 10, CAT_NEGOCIOS,
                "https://covers.openlibrary.org/b/isbn/9788576840220-L.jpg"));

        log.info("[DEV] Books seeded — {} created, {} already present", created, TOTAL_BOOKS - created);
    }

    /** Returns 1 when the book was created, 0 when the catalog already had it. */
    private int seedBook(CreateBookCommand command) {
        try {
            createBookUseCase.execute(command);
            return 1;
        } catch (IsbnAlreadyExistsException e) {
            return 0;
        }
    }
}
