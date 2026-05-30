# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Commands

```bash
# Build all modules
./mvnw clean install

# Build skipping tests
./mvnw clean install -DskipTests

# Run the application (dev profile with H2)
./mvnw spring-boot:run -pl bookland-app

# Run all tests
./mvnw test

# Run tests for a single module
./mvnw test -pl bookland-user

# Run a single test class
./mvnw test -pl bookland-user -Dtest=UserDomainServiceTest

# Run with Docker (prod profile, PostgreSQL)
docker-compose up --build
```

**Dev endpoints:**
- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 Console: `http://localhost:8080/h2-console` (JDBC URL: `jdbc:h2:mem:booklanddb`)

## Git Conventions

All commits must follow **[Conventional Commits](https://www.conventionalcommits.org/)**:

```
<type>[optional scope]: <short description>

[optional body]

[optional footer(s)]
```

| Type | When to use |
|---|---|
| `feat` | New feature or endpoint |
| `fix` | Bug fix |
| `refactor` | Code change that is neither a fix nor a feature |
| `test` | Adding or updating tests |
| `docs` | Documentation only |
| `build` | Build system or dependency changes (pom.xml, Docker) |
| `chore` | Miscellaneous maintenance (no production code change) |
| `ci` | CI/CD configuration |

**Scope** is optional and should be the domain module name when relevant (e.g., `feat(user)`, `fix(auth)`, `build(catalog)`).

**Breaking changes** must be marked with `!` after the type/scope (`feat(auth)!:`) and explained in the footer with `BREAKING CHANGE:`.

## Architecture

### Module Structure

```
bookland/               ← Parent POM (dependency management)
├── bookland-app/       ← Spring Boot bootstrap only; imports all domain modules
└── bookland-{domain}/  ← One module per domain (user, auth, catalog, orders, reviews, inventory, wishlist)
```

`bookland-app` has no business logic — it exists solely to assemble all domain modules and host `application.yml`. The `spring-boot-maven-plugin` runs only here.

### Layered Package Layout (per domain)

Each domain module (`bookland-user`, `bookland-auth`, etc.) follows strict Clean Architecture layers:

```
com.devrenno.bookland.{domain}/
├── domain/
│   ├── entity/         ← Pure Java domain objects (no framework annotations)
│   ├── valueobject/    ← Immutable value types (e.g. Email, UserId)
│   ├── service/        ← Domain rules; no I/O, no Spring
│   ├── repository/     ← (marker/unused; ports preferred)
│   └── exception/      ← Domain-specific exceptions
├── application/
│   ├── annotation/     ← @UseCase (meta-annotation for @Service)
│   ├── controller/     ← *ApplicationController: implements use-case interfaces, orchestrates domain
│   ├── dto/            ← Commands and responses (application-layer DTOs)
│   └── port/
│       ├── in/         ← Use-case interfaces (e.g. RegisterUserUseCase)
│       └── out/        ← Outbound port interfaces (e.g. UserPersistencePort, PasswordEncoderPort)
├── infrastructure/
│   ├── config/         ← Spring @Configuration beans (wires domain services, security)
│   ├── persistence/    ← JPA entities, Spring Data repos, adapters implementing out-ports
│   ├── security/       ← JWT filter, BCrypt adapter
│   └── adapter/        ← Other adapter implementations
└── api/
    ├── controller/     ← @RestController; delegates to use-case interfaces only
    ├── dto/            ← HTTP request/response DTOs (separate from application DTOs)
    └── mapper/         ← MapStruct mappers between API DTOs and application DTOs
```

### Key Design Rules

**Two controller types, two roles:**
- `*ApplicationController` — annotated `@UseCase`; implements all use-case `port/in` interfaces; contains actual business orchestration. The domain's only entry point for logic.
- `*Controller` (`@RestController`) — HTTP adapter only; maps HTTP → use-case call → HTTP response. Must depend only on `port/in` interfaces, never on `*ApplicationController` directly.

**Domain layer has zero Spring/JPA dependencies.** Domain entities and services are plain Java. They are wired in `infrastructure/config` via `@Bean` methods (e.g., `UserDomainConfig`).

**Cross-domain dependencies are explicit and intentional:** `bookland-auth` depends on `bookland-user` to reuse the user model via `UserLookupPort` (adapts `GetUserByEmailUseCase`). No other cross-domain coupling exists.

**Port/Adapter pattern for all I/O:** persistence, password encoding, JWT generation, and user lookup are all accessed through interfaces defined in `application/port/out/`. Infrastructure adapters implement those interfaces.

### Auth Flow

`bookland-auth` handles JWT: `POST /api/v1/auth/login` → `AuthController` → `LoginUseCase` → `AuthApplicationController` → validates credentials via `UserLookupPort` + `PasswordEncoder` → issues JWT via `TokenProviderPort`. The `JwtAuthenticationFilter` (in `bookland-auth`) intercepts every request and populates `SecurityContextHolder`. Security config lives in `bookland-auth`'s `SecurityConfig`.

Public endpoints: `POST /api/v1/auth/**`, `POST /api/v1/users`, `/h2-console/**`, `/swagger-ui/**`, `/api-docs/**`.

### Technology Notes

- **Java 21**, Spring Boot 4.0.6
- **MapStruct** for all struct-to-struct mapping (configured with `defaultComponentModel=spring`; Lombok binding order matters — Lombok processor must come before MapStruct in `annotationProcessorPaths`)
- **H2** in dev (`spring.profiles.active=dev`), **PostgreSQL 16** in prod
- **JJWT 0.12.6** for JWT; secret and expiration configured under `bookland.jwt.*`
- All domain modules use `spring-boot-starter-webmvc-test` for slice testing (`@WebMvcTest`) plus `spring-boot-starter-test` (JUnit 5 + Mockito + AssertJ)
