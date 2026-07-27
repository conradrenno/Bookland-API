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

# Run with Docker (prod profile, PostgreSQL + Flyway)
docker-compose up --build

# Start from an empty database (wipes the pgdata and covers volumes)
docker compose down -v && docker compose up --build
```

The Dockerfile enumerates every module twice (one `COPY` for the `pom.xml`, one for `src`) to keep the dependency-download layer cacheable. **That list duplicates `pom.xml` and nothing enforces it** — a new module must be added there too, or the image build fails. `.dockerignore` keeps `target/`, `.git`, `bookland-data/` and `.env` out of the build context.

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
├── bookland-web-support/ ← Platform library: HTTP error-contract glue (see below)
└── bookland-{domain}/  ← One module per domain (user, auth, catalog, orders, reviews, inventory, wishlist, payments)
```

`bookland-web-support` is **not** a shared kernel — the "duplicate it per module" rule (PageQuery, PageResult) still holds for anything with domain meaning. It holds framework glue only: `ProblemDetails`, `ProblemDetailWriter`, `AuthErrorCode`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`. It must never contain a domain type or depend on another `bookland-*` module, and only `*.infrastructure` packages may import it. It exists so the HTTP error contract survives a future split into separate services.

`bookland-app` has no business logic — it exists solely to assemble all domain modules and host `application.yml`. The `spring-boot-maven-plugin` runs only here.

### Layered Package Layout (per domain) — 4-layer Clean Architecture

Each domain module follows **four** layers. **Domain, Application and Adapters are framework-free** (Lombok is allowed — it is source-only and leaves no bytecode trace). Only **Infrastructure** may touch Spring / JPA / Jackson. Dependencies always point inward: `infrastructure → adapters → application → domain`.

> All 8 domain modules follow this model (migration from the previous 3-layer `@UseCase` + `api/` layout completed in July 2026). `bookland-user` is the reference implementation — follow its shape for all new code.

```
com.devrenno.bookland.{domain}/
├── domain/                 [framework-free]
│   ├── entity/             ← Pure Java; static factories (create/reconstitute) encapsulate intrinsic invariants; private ctor, no public setters
│   ├── valueobject/        ← Immutable value types (e.g. Email, UserId)
│   ├── service/            ← Domain rules needing cross-aggregate/lookup data (e.g. email uniqueness); no I/O, no Spring
│   └── exception/          ← Domain-specific exceptions
├── application/            [framework-free]
│   ├── service/            ← *Service: implements port/in; RETURNS DOMAIN ENTITIES; private ctor + static create(...) factory (no @UseCase)
│   ├── dto/                ← Input commands + query read-models (no output DTOs — use cases return entities or read-models)
│   ├── common/             ← PageQuery / PageResult (framework-free pagination, duplicated per module)
│   └── port/
│       ├── in/             ← Use-case interfaces (e.g. RegisterUserUseCase) — return domain entities
│       └── out/            ← Outbound ports (UserPersistencePort, PasswordEncoderPort, TransactionPort)
├── adapters/               [framework-free]
│   ├── controller/         ← Internal controller: orchestrates use cases + presenter; also the module's composition root (static create(ports) wires the inner graph)
│   ├── presenter/          ← Plain-Java presenters: domain entity → ViewModel
│   └── viewmodel/          ← Output DTOs (no Jackson); delivered as-is by the HTTP layer
└── infrastructure/         [Spring]
    ├── web/                ← @RestController (delegates to internal controller), request DTOs, request mappers (MapStruct), @RestControllerAdvice
    ├── config/             ← Composition root beans: 1 @Bean per module entry point calling *Controller.create(ports) + cross-module use-case beans
    ├── persistence/        ← JPA entities, Spring Data repos, adapters implementing out-ports, persistence mappers (JPA entity ⇄ domain via reconstitute)
    ├── adapter/            ← Cross-module adapters: implement this module's out-ports by calling other modules' in-port beans (or the reverse — e.g. orders implements catalog's ActiveOrderCheckPort)
    ├── transaction/        ← TransactionAdapter (TransactionTemplate) implementing TransactionPort, where the module needs it
    └── security/           ← JWT filter, BCrypt adapter (user/auth modules)
```

### Key Design Rules

**Framework-free inner layers, enforced by ArchUnit.** Domain, Application and Adapters must not depend on `org.springframework..`, `jakarta.persistence..` or `com.fasterxml.jackson..`. Each module has an `ArchitectureRulesTest` (see `bookland-user`) that fails the build on violation, plus a `layeredArchitecture` rule enforcing the inward dependency direction. There is **no `@UseCase` annotation** — services are plain Java.

**Two controllers, two roles:**
- **Internal controller** (`adapters/controller`) — plain Java. Orchestrates `port/in` use cases and calls the Presenter to produce a `ViewModel`. Is also the module's **composition root**: its static `create(...)` factory receives the outbound ports (as interfaces) and manually wires the domain service + use cases + presenter.
- **API controller** (`infrastructure/web`, `@RestController`) — HTTP adapter only. Maps HTTP → internal controller call → `ResponseEntity<ViewModel>`. Depends only on the internal controller, never on `*Service`.

**Use cases return domain entities**, not DTOs. Output shaping happens in the Presenter (→ ViewModel). Cross-module consumers depend on the source module's `port/in` and receive its **domain entities** (e.g. `bookland-auth` maps the `User` entity from `GetUserByEmailUseCase`/`RegisterUserUseCase` into its own `AuthUserDto`).

**Exception — use cases whose output needs data from another module return a query read-model** from `application/dto/` instead of the entity, assembled in the application layer from the aggregate + an out-port lookup (`CartView`/`CartItemView` via `BookInfoPort`, `WishlistView` via `WishlistBookInfoPort`, `ReviewView`/`ReviewList` via `CustomerNamePort`, `LowStockBook` via `LowStockBooksPort`). The assemblers (`CartViewAssembler`, `WishlistViewAssembler`, `ReviewViewAssembler`) are package-private in `application/service/` and **degrade gracefully** when the lookup finds nothing — a cart/wishlist item whose book left the catalog renders as `"Unavailable"`/`available: false` rather than failing the whole response.

**Manual wiring (composition root), no `@UseCase`/`@Service` on inner classes.** Infrastructure creates only the outbound-port adapters (`@Repository`/`@Component`) and exposes **one `@Bean`** per module entry point that calls `*Controller.create(ports)`. Inner classes are never Spring beans and never self-annotate. Never instantiate an infrastructure adapter from inside an inner layer.

**Cross-module use cases must be explicit `@Bean`s.** A use case consumed by another module (e.g. orders' `VerifyPurchaseUseCase` → reviews, `AddCartItemUseCase` → wishlist, catalog's `GetBookByIdUseCase` → orders/reviews/wishlist) must be exposed as its own `@Bean` in the module's `*BeansConfig` — forgetting one fails context startup in the consumer's adapter.

**Soft-deleted books are invisible outside the catalog.** `GetBookByIdUseCase` — the in-port every other module reads books through — filters out inactive books, so a removed book cannot be fetched (404), added to a cart/wishlist (404) or checked out (409 `CartItemUnavailableException`, via `BookInfoPort.findBookInfo` returning empty). Admin write flows (update / cover upload / removal) bypass it and go straight to `BookPersistencePort.findById`, which still sees inactive books. `available` on a `BookViewModel` means `active && stockQuantity > 0`.

**Bean names must be unique across modules.** Adapters duplicated per module with the same simple class name (e.g. `TransactionAdapter` in wishlist and orders) collide under component scanning — give the later one an explicit name: `@Component("ordersTransactionAdapter")`.

**Transactions are framework-free** via `TransactionPort` (outbound port, `inTransaction(Supplier<T>)`) implemented in infrastructure with `TransactionTemplate`. Do **not** put `@Transactional` on application services (breaks framework-freedom and does not work without a Spring proxy under manual wiring). No-rollback semantics (e.g. checkout must commit the `PAYMENT_FAILED` order on a declined payment) are expressed by **returning** an outcome from the transaction and throwing the exception after the commit — see `CheckoutService`.

**Pagination is framework-free** via `PageQuery`(page, size) and `PageResult<T>` in each module's `application/common/` (deliberately duplicated per module — no shared kernel). Persistence adapters translate `PageQuery ↔ PageRequest` and `Page ↔ PageResult`; fixed sort orders live in the adapter. `PageResult<ViewModel>` is also the paged HTTP response envelope (content/page/size/totalElements/totalPages).

**Port/Adapter pattern for all I/O:** persistence, password encoding, JWT generation, transactions, cross-module lookup and image storage are all accessed through interfaces in `application/port/out/`; infrastructure adapters implement them. Cover images are stored via `ImageStoragePort` (catalog out-port) — the `LocalImageStorageAdapter` writes bytes to `bookland.storage.covers-location` and returns a public `/media/covers/...` path served by `MediaResourceConfig`; swap in an S3/GCS adapter without touching inner layers. `MultipartFile` never crosses the web layer: `BookApiController` extracts `byte[]` + filename + contentType into a framework-free `UploadBookCoverCommand`.

### Auth Flow

`bookland-auth` owns authentication: `POST /api/v1/auth/login` → `AuthApiController` → internal `AuthController` → `LoginUseCase` (`LoginService`) → validates credentials via `UserLookupPort` (→ user module) + `PasswordEncoderPort` → issues an access JWT via `TokenProviderPort` **plus a persisted, opaque refresh token** (table `refresh_tokens`). `POST /auth/refresh` rotates the pair (single-use: the used refresh token is revoked); `POST /auth/logout` revokes it; `POST /auth/register` creates the user via the user module's `RegisterUserUseCase` (role always CUSTOMER) and authenticates immediately. There is no user-creation endpoint in the user module itself.

The `JwtAuthenticationFilter` (in `bookland-auth`) intercepts every request, validates the Bearer token and populates `SecurityContextHolder` — with the **userId stored in `Authentication.getDetails()`**, which controllers read via `extractUserId(Principal)`. All authorization rules for every module live in `bookland-auth`'s `SecurityConfig` (rule order matters: specific admin routes are declared before broad permitAll patterns).

**401 and 403 are distinct and must stay that way** — see `docs/error-contract.md`, which is the contract clients code against. A missing/expired/invalid token is **401** (`TOKEN_MISSING` / `TOKEN_EXPIRED` / `TOKEN_INVALID`); an authenticated caller without the role is **403** (`INSUFFICIENT_ROLE`). Both are `application/problem+json` with a machine-readable `code`. This only works because `SecurityConfig` wires `.exceptionHandling(...)`: without it Spring Security falls back to `Http403ForbiddenEntryPoint` and answers *everything* with an empty 403, which is indistinguishable to a client. The filter never writes a response — it records the rejection reason under `AuthErrorCode.REQUEST_ATTRIBUTE` and lets `RestAuthenticationEntryPoint` render it. A rejected token on a **public** endpoint does not fail the request. `AuthErrorContractIntegrationTest` (bookland-app) locks all of this against the real filter chain.

Public endpoints: `POST /api/v1/auth/**`, `GET /api/v1/books/**`, `GET /api/v1/categories/**`, `GET /media/**` (stored cover images), `/h2-console/**`, `/swagger-ui/**`, `/api-docs/**`. Admin-only (`ROLE_ADMIN`): book/inventory writes including cover upload (`POST /api/v1/books/{id}/cover`, `multipart/form-data`, part `file`), `/api/v1/admin/**` — which includes the order back-office: `GET /api/v1/admin/orders?status=&page=&size=` (all orders, newest first, `AdminOrderSummaryViewModel` rows carrying `customerId`), `GET /api/v1/admin/orders/{id}`, `GET /api/v1/admin/orders/customer/{customerId}` and `PATCH /api/v1/admin/orders/{id}/status`. Everything else requires authentication.

### Technology Notes

- **Java 21**, Spring Boot 4.0.6
- **MapStruct** for all struct-to-struct mapping (configured with `defaultComponentModel=spring`; Lombok binding order matters — Lombok processor must come before MapStruct in `annotationProcessorPaths`)
- **H2** in dev (`spring.profiles.active=dev`), **PostgreSQL 16** in prod. Both JDBC drivers are declared in `bookland-app` (the assembly module), not in a domain module
- **Flyway owns the schema in both profiles**, and `ddl-auto` is `validate` in both (Flyway creates, Hibernate verifies the mapping and refuses to boot on a drift). Migrations live in `bookland-app/src/main/resources/db/migration`, versioned by **timestamp** (`V20260726164500__init_schema.sql`) so parallel branches cannot collide. Dev runs the same migrations against H2 opened with `MODE=PostgreSQL` — so **migration SQL must stay in the PostgreSQL/H2 common subset**, and a migration that breaks fails on the next dev boot rather than in prod. There is no `import.sql`; the categories are reference data in `V2`. **Boot 4 gotcha:** `flyway-core` alone does nothing — the auto-configuration lives in `spring-boot-flyway`, so the dependency must be `spring-boot-starter-flyway` (plus `flyway-database-postgresql`; H2 support is inside `flyway-core`). Without it Flyway fails silently: no error, no migrations applied
- **`AdminBootstrap` and `DevDataLoader` must both stay idempotent** — they look up before inserting (`GetUserByEmailUseCase`; `IsbnAlreadyExistsException` per book). The in-memory H2 survives a devtools restart (`DB_CLOSE_DELAY=-1`) and Flyway no longer wipes it as `create-drop` did, so a non-idempotent seed breaks the second start. The category UUIDs in `V2` are referenced literally by `DevDataLoader`'s constants — changing them breaks the book seed
- **Datasource URL is `${DB_URL:jdbc:postgresql://localhost:5432/bookland}`.** `docker-compose.yml` injects `DB_URL` pointing at the `postgres` service name; the default covers running the app from the host against the compose Postgres (port 5432 is published)
- **FKs exist only within a module.** Cross-module columns (`cart_items.book_id`, `orders.customer_id`, `payments.order_id`, `refresh_tokens.user_id`, …) are indexed `uuid` with no constraint, mirroring the absence of cross-module JPA relationships. Do not add them without discussing the module-split implications
- **JJWT 0.12.6** for JWT; secret and expirations configured under `bookland.jwt.*`
- **Admin bootstrap**: `AdminBootstrap` (bookland-app, `@Order(1)`, all profiles) guarantees exactly one admin user on startup — credentials under `bookland.admin.email/password` (prod: `ADMIN_EMAIL`/`ADMIN_PASSWORD` env vars). `DevDataLoader` (`@Order(2)`, dev only) seeds a sample customer and books
- **Cover image storage**: local filesystem in dev/single-node. Location under `bookland.storage.covers-location` (dev: `./bookland-data/covers`; prod: `STORAGE_COVERS_LOCATION` env, default `/var/bookland/covers` — mount a volume so uploads survive restarts). Upload limits under `spring.servlet.multipart.*` (5 MB); allowed types JPEG/PNG/WEBP validated in `UploadBookCoverService`
- **ArchUnit** (`archunit-junit5`, test scope) enforces framework-freedom of the inner layers and the inward dependency direction — one `ArchitectureRulesTest` per domain module
- **Tests** are plain JUnit 5 + Mockito + AssertJ unit tests against mocked ports (`TransactionPort` is faked with a pass-through, not mocked); `BooklandApplicationTests` (bookland-app, `@SpringBootTest`) boots the full context and validates all composition-root wiring. There are no `@WebMvcTest` slices
