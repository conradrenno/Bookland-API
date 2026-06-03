package com.devrenno.bookland;

import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.port.in.CreateBookUseCase;
import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.port.in.RegisterUserUseCase;
import com.devrenno.bookland.user.domain.entity.UserRole;
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

    private final RegisterUserUseCase registerUserUseCase;
    private final CreateBookUseCase createBookUseCase;

    public DevDataLoader(RegisterUserUseCase registerUserUseCase, CreateBookUseCase createBookUseCase) {
        this.registerUserUseCase = registerUserUseCase;
        this.createBookUseCase = createBookUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        seedUsers();
        seedBooks();
    }

    private void seedUsers() {
        registerUserUseCase.execute(new CreateUserCommand(
                "João Silva", "joao@bookland.com", "joao1234", UserRole.CUSTOMER));
        log.info("[DEV] Users seeded — joao@bookland.com (joao1234)");
    }

    private void seedBooks() {
        createBookUseCase.execute(new CreateBookCommand(
                "Clean Code",
                "978-0132350884",
                List.of("Robert C. Martin"),
                "Prentice Hall", 2008, "1ª edição",
                "Um guia de boas práticas para escrita de código limpo e sustentável.",
                BigDecimal.valueOf(44.90), 20, CAT_TECNOLOGIA));

        createBookUseCase.execute(new CreateBookCommand(
                "The Pragmatic Programmer",
                "978-0135957059",
                List.of("David Thomas", "Andrew Hunt"),
                "Addison-Wesley", 2019, "20th Anniversary Edition",
                "Seu caminho para se tornar um programador pragmático e eficaz.",
                BigDecimal.valueOf(59.90), 15, CAT_TECNOLOGIA));

        createBookUseCase.execute(new CreateBookCommand(
                "Design Patterns",
                "978-0201633610",
                List.of("Erich Gamma", "Richard Helm", "Ralph Johnson", "John Vlissides"),
                "Addison-Wesley", 1994, "1ª edição",
                "Os 23 padrões de projeto que todo desenvolvedor precisa conhecer.",
                BigDecimal.valueOf(79.90), 8, CAT_TECNOLOGIA));

        createBookUseCase.execute(new CreateBookCommand(
                "Duna",
                "978-8576574675",
                List.of("Frank Herbert"),
                "Aleph", 1965, "Edição especial",
                "A épica saga de sobrevivência e poder no planeta deserto Arrakis.",
                BigDecimal.valueOf(49.90), 12, CAT_FICCAO));

        createBookUseCase.execute(new CreateBookCommand(
                "O Investidor Inteligente",
                "978-8576840220",
                List.of("Benjamin Graham"),
                "HarperCollins", 1949, "Ed. revisada",
                "O guia definitivo de investimento em valor de Benjamin Graham.",
                BigDecimal.valueOf(54.90), 10, CAT_NEGOCIOS));

        log.info("[DEV] Books seeded — 5 books available in catalog");
    }
}
