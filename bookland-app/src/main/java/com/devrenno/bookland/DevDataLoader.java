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

    // All eight category UUIDs inserted by V20260726164600__reference_categories.sql.
    // The migration's own header still says only three of them are referenced here —
    // it predates this seed and cannot be edited without changing its Flyway checksum.
    private static final UUID CAT_TECNOLOGIA  = UUID.fromString("c3d4e5f6-a7b8-9012-cdef-123456789012");
    private static final UUID CAT_FICCAO      = UUID.fromString("a1b2c3d4-e5f6-7890-abcd-ef1234567890");
    private static final UUID CAT_NEGOCIOS    = UUID.fromString("e5f6a7b8-c9d0-1234-efab-345678901234");
    private static final UUID CAT_ROMANCE     = UUID.fromString("b2c3d4e5-f6a7-8901-bcde-f12345678901");
    private static final UUID CAT_HISTORIA    = UUID.fromString("d4e5f6a7-b8c9-0123-defa-234567890123");
    private static final UUID CAT_AUTOAJUDA   = UUID.fromString("f6a7b8c9-d0e1-2345-fabc-456789012345");
    private static final UUID CAT_LIT_BRASIL  = UUID.fromString("a7b8c9d0-e1f2-3456-abcd-567890123456");
    private static final UUID CAT_INFANTIL    = UUID.fromString("b8c9d0e1-f2a3-4567-bcde-678901234567");

    private static final String CUSTOMER_EMAIL = "joao@bookland.com";
    private static final int TOTAL_BOOKS = 10;

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
                "978-8576573135",
                List.of("Frank Herbert"),
                "Aleph", 2017, "Edição especial",
                "A épica saga de sobrevivência e poder no planeta deserto Arrakis.",
                BigDecimal.valueOf(49.90), 12, CAT_FICCAO,
                "https://covers.openlibrary.org/b/isbn/9788576573135-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "Pai Rico, Pai Pobre",
                "978-8550801483",
                List.of("Robert T. Kiyosaki", "Sharon L. Lechter"),
                "Alta Books", 2017, "Edição de 20 anos",
                "O que os ricos ensinam a seus filhos sobre dinheiro — e que os pobres não ensinam.",
                BigDecimal.valueOf(54.90), 10, CAT_NEGOCIOS,
                "https://covers.openlibrary.org/b/isbn/9788550801483-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "Dom Casmurro",
                "978-8594318602",
                List.of("Machado de Assis"),
                "Principis", 2019, "1ª edição",
                "O ciúme de Bentinho e o eterno enigma dos olhos de ressaca de Capitu.",
                BigDecimal.valueOf(19.90), 25, CAT_LIT_BRASIL,
                "https://covers.openlibrary.org/b/isbn/9788594318602-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "Cem Anos de Solidão",
                "978-8501012074",
                List.of("Gabriel García Márquez"),
                "Record", 2022, "1ª edição",
                "A saga da família Buendía em Macondo, obra-prima do realismo mágico.",
                BigDecimal.valueOf(69.90), 14, CAT_ROMANCE,
                "https://covers.openlibrary.org/b/isbn/9788501012074-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "Sapiens: Uma Breve História da Humanidade",
                "978-8525432186",
                List.of("Yuval Noah Harari"),
                "L&PM", 2015, "1ª edição",
                "Como um primata irrelevante se tornou o senhor do planeta.",
                BigDecimal.valueOf(64.90), 18, CAT_HISTORIA,
                "https://covers.openlibrary.org/b/isbn/9788525432186-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "O Poder do Hábito",
                "978-8539004119",
                List.of("Charles Duhigg"),
                "Objetiva", 2012, "1ª edição",
                "Por que fazemos o que fazemos na vida e nos negócios.",
                BigDecimal.valueOf(47.90), 22, CAT_AUTOAJUDA,
                "https://covers.openlibrary.org/b/isbn/9788539004119-L.jpg"));

        created += seedBook(new CreateBookCommand(
                "Harry Potter e a Pedra Filosofal",
                "978-8532530783",
                List.of("J. K. Rowling"),
                "Rocco", 2017, "Edição especial",
                "O menino que sobreviveu descobre que é um bruxo e parte para Hogwarts.",
                BigDecimal.valueOf(39.90), 30, CAT_INFANTIL,
                "https://covers.openlibrary.org/b/isbn/9788532530783-L.jpg"));

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
