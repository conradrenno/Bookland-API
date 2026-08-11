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

`bookland-web-support` is **not** a shared kernel — the "duplicate it per module" rule (PageQuery, PageResult) still holds for anything with domain meaning. It holds framework glue only: `ProblemDetails`, `ProblemDetailWriter`, `AuthErrorCode`, `RestAuthenticationEntryPoint`, `RestAccessDeniedHandler`, `ValidationExceptionHandler`, `ValidationConfig`, `ErrorResponsesCustomizer`, `AuthenticatedUser` + `AuthenticatedUserArgumentResolver` + `AuthenticatedUserConfig`. It must never contain a domain type or depend on another `bookland-*` module, and only `*.infrastructure` packages may import it. It exists so the HTTP error contract survives a future split into separate services.

**All error responses follow `docs/error-contract.md`** — `application/problem+json` with a machine-readable `code`, messages in **English**. Bean validation is handled by the **single** `ValidationExceptionHandler` in `bookland-web-support` (`@Order(HIGHEST_PRECEDENCE)`), which emits an `errors` map (field → messages) so a client can render errors inline; **a module must never add its own `MethodArgumentNotValidException` handler** — two advices for the same exception leaves the winner up to bean ordering and the payload shape drifts per module. Module advices keep owning their business exceptions (404/409/422) and build them through `ProblemDetails.of(status, detail, code)` — every error response in the API carries a `code`, and the table of business codes is in `docs/error-contract.md`. Note that **not every 403 means "wrong role"**: `ORDER_ACCESS_DENIED` and `PURCHASE_REQUIRED` are business 403s, only `INSUFFICIENT_ROLE` is about authorities. `ValidationConfig` pins constraint messages to English: Hibernate Validator otherwise resolves them against the host JVM's default locale, so the same build answered Portuguese on a pt-BR developer machine and English in the container. `OpenApiConfig` (bookland-app) publishes the contract in `/api-docs` — `ProblemDetail`/`ValidationProblemDetail` schemas, a `default` error response on every operation, an explicit `400` wherever a request has input, and the `bearerAuth` scheme. Per-operation 401/403/404/409 are deliberately **not** enumerated (they depend on `SecurityConfig` and on domain exceptions no annotation declares, so a hand-kept list goes stale silently). **Every request-DTO `String` bound to a `varchar(n)` column needs `@Size(max = n)`.** Nothing enforces this — Hibernate does not validate length before the insert, so a missing bound turns a bad request into a `DataIntegrityViolationException`, i.e. a 500 where a 400 naming the field belongs. Fields backed by `text` (`books.synopsis`, `reviews.comment`) are deliberately unbounded. Note `books.cover_image_url` and `order_items.cover_image_url` are both `varchar(255)`: raising the DTO bound without migrating **both** columns just moves the 500 to checkout.

**A handler answering anything other than 200 must carry `@ResponseStatus`** (e.g. `@ResponseStatus(HttpStatus.CREATED)` next to `ResponseEntity.status(HttpStatus.CREATED)`): springdoc infers 200 from the return type and cannot see through `ResponseEntity.status(...)`, so without it the published document claims 200 and every generated client gets the wrong success type. The annotation is redundant at runtime — `ResponseEntity` wins — and exists to keep the document honest. The eight current cases are pinned in `OpenApiErrorContractIntegrationTest`, and **`WebLayerRulesTest`** (ArchUnit in bookland-app, the only place with every module on the classpath) fails the build on a handler in `..infrastructure.web..` that calls `ResponseEntity.status/created/noContent/accepted` without `@ResponseStatus`.

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

`bookland-auth` **hosts a Spring Authorization Server** (OAuth2 + OIDC) and every other module is a Resource Server validating the tokens it issues — one process, one database, no BFF. Login is `authorization_code` + PKCE: the browser goes to `/oauth2/authorize`, authenticates against `BooklandUserDetailsService` (→ `UserLookupPort` → user module) through Spring's own `DaoAuthenticationProvider`, and the client exchanges the code at `/oauth2/token` for an access token, an `id_token` and a refresh token. Refreshing is `grant_type=refresh_token` at the same endpoint, single-use; ending a session is `/connect/logout`. **There is no `/api/v1/auth/login`, `/refresh` or `/logout`** — those were a hand-written implementation of the above and were deleted.

`POST /api/v1/auth/register` survives, because registering is business logic and not authentication: e-mail uniqueness, the default CUSTOMER role and the invariants of `User.create` have nowhere to live inside a protocol endpoint. It answers **201 with the account** (`id`, `email`, `role`) and **no credential**, and establishes the Authorization Server's session (`RegistrationSessionEstablisher`) so the client can go straight to `/oauth2/authorize` without a second password prompt. There is no user-creation endpoint in the user module itself.

**`sub` carries the internal `UserId`, never the e-mail** — written by `BooklandTokenCustomizer` into *both* tokens, with no filter by token type. This is not a preference: OIDC requires `sub` to be stable and never reassigned, every business column stores the `UserId`, and after the old filter's removal `sub` is the only channel identity travels through. The customizer reads the id from `BooklandUserDetails`, a custom `UserDetails` that exists because the framework's interface has nowhere to put an id. Two consequences that are easy to get wrong and are documented at length in `docs/oauth2-customizations.md`: a custom principal must be added to the polymorphic-typing allowlist (`AuthorizationJsonMapperFactory` + `BooklandUserDetailsMixin`) or the code exchange 500s, and it must implement `CredentialsContainer` or the BCrypt hash is written into `oauth2_authorization.attributes`.

**The access token carries `aud = bookland-api`; the `id_token` keeps the client id.** Both halves are required: the generator gives both tokens the same `aud` by default and the default decoder validates none, so without `BooklandTokenCustomizer` *and* `ApiAudienceValidator` an `id_token` works as a Bearer credential at the API. Pinned by `AuthorizationCodeFlowIntegrationTest`.

Signing is **RS256 with an RSA pair read from `bookland.oauth2.jwk.*`** (base64 DER, single-line so it fits an env var), published at `/oauth2/jwks`. The private half never leaves the signing path — unlike the HMAC secret it replaced, which let every component able to *validate* a token also *mint* one.

**Four `SecurityFilterChain`s, and the order is load-bearing** — all in `bookland-auth`:

| Order | Matches | Session | Purpose |
|---|---|---|---|
| 1 | the configurer's own endpoint matcher (`/oauth2/*`, `/userinfo`, discovery) | stateful | the Authorization Server; also a resource server, because `/userinfo` is itself token-protected |
| 2 | `/login`, `/login/**` | stateful | the form login (Spring's generated page for now) |
| 3 | `/error`, `/media/**`, `/h2-console/**`, swagger, `/api-docs/**` | stateless | **no resource server** — see below |
| 4 | everything else | stateless | the API, as a resource server |

Chain 3 exists because the resource server rejects an unusable Bearer token *wherever* it appears, while the filter it replaced recorded the reason and carried on. On the `ERROR` dispatch that turns a real 500 into a 401, which is the disguised-session bug this project already fixed once. `/api/v1/books` and `/api/v1/categories` are deliberately **not** in chain 3: their public reads share a path prefix with admin writes, and a chain claiming them by path would have to reproduce the method-by-method rules to avoid opening a write route.

All authorization rules for every module live in chain 4's `authorizeHttpRequests` (rule order matters: specific admin routes are declared before broad permitAll patterns). `JwtAuthenticationConverter` maps the `role` claim to a `ROLE_*` authority — without it every token is valid, every caller is authenticated and carries no authority at all, so every admin route answers 403 including to an admin.

**A handler that needs to know who is calling declares an `AuthenticatedUser` parameter** — never `Principal`, and never `SecurityContextHolder` directly. `AuthenticatedUserArgumentResolver` (bookland-web-support, registered by `AuthenticatedUserConfig`) is the single place that knows how identity rides on the `Authentication`, which is precisely what changes if the authentication mechanism does (OAuth2 resource server: a claim read; external IdP: a lookup from the issuer's `sub`). It **never returns null** — no caller is an `AuthenticationCredentialsNotFoundException` that `ExceptionTranslationFilter` renders as the contract's 401, and an authentication carrying no UUID is an `IllegalStateException`, i.e. the 500 a wiring bug deserves. The predecessor was an `instanceof` over `getDetails()` copy-pasted into six controllers that returned `null` on mismatch, so a filter change would have silently attributed carts and orders to a null customer instead of failing. **`AuthenticatedUser` must stay registered in `OpenApiConfig`'s `SpringDocUtils.addRequestWrapperToIgnore`** — to springdoc an unannotated POJO parameter is a set of query parameters, and without it the document publishes `caller` as a *required* query param on every such operation. Pinned by `AuthenticatedUserArgumentResolverTest` and `OpenApiErrorContractIntegrationTest`.

**401 and 403 are distinct and must stay that way** — see `docs/error-contract.md`, which is the contract clients code against. A missing/expired/invalid token is **401** (`TOKEN_MISSING` / `TOKEN_EXPIRED` / `TOKEN_INVALID`); an authenticated caller without the role is **403** (`INSUFFICIENT_ROLE`). Both are `application/problem+json` with a machine-readable `code`. This only works because `SecurityConfig` wires `.exceptionHandling(...)`: without it Spring Security falls back to `Http403ForbiddenEntryPoint` and answers *everything* with an empty 403, which is indistinguishable to a client. `RestAuthenticationEntryPoint` renders the body from a reason recorded under `AuthErrorCode.REQUEST_ATTRIBUTE`; what records it is now `BearerTokenErrorClassifier`, which wraps that entry point and reads the cause out of the exception chain — and it must be registered **inside** `.oauth2ResourceServer(...)` as well as in `.exceptionHandling(...)`, because the resource server installs its own entry point that answers with an empty body. `TOKEN_EXPIRED` is told apart from `TOKEN_INVALID` by `AccessTokenExpiryValidator`, which exists solely to fail expiry under an error code of our own: the framework's `JwtTimestampValidator` reports it as `invalid_token` like everything else, leaving an English message as the only discriminator. **A rejected token on a public endpoint now *does* fail the request** — `GET /api/v1/books` with an expired token answers 401. `AuthErrorContractIntegrationTest` (bookland-app) locks all of this against the real filter chain.

Public endpoints: `POST /api/v1/auth/register`, `/oauth2/**`, `/login`, `/.well-known/**`, `GET /api/v1/books/**`, `GET /api/v1/categories/**`, `GET /media/**` (stored cover images), `/error`, `/h2-console/**`, `/swagger-ui/**`, `/api-docs/**`.

**`/error` must stay `permitAll`.** Boot registers the security chain for the `ERROR` dispatch too, so when an unhandled exception makes the container forward to `/error`, an authenticated `/error` answers the *forward* with `401 TOKEN_MISSING` — the real 500 never reaches the client, and arrives disguised as an expired session that makes clients refresh and then log the user out. `ProblemDetailErrorController` (bookland-web-support) renders that dispatch as problem+json with `code: INTERNAL_ERROR` and `instance` set to the **original** request path; a 5xx never echoes the exception message (stack traces, SQL, column names). Pinned by `ProblemDetailErrorControllerTest` and `BusinessErrorContractIntegrationTest`. Admin-only (`ROLE_ADMIN`): book/inventory writes including cover upload (`POST /api/v1/books/{id}/cover`, `multipart/form-data`, part `file`), `/api/v1/admin/**` — which includes the order back-office: `GET /api/v1/admin/orders?status=&page=&size=` (all orders, newest first, `AdminOrderSummaryViewModel` rows carrying `customerId`), `GET /api/v1/admin/orders/{id}`, `GET /api/v1/admin/orders/customer/{customerId}` and `PATCH /api/v1/admin/orders/{id}/status`. Everything else requires authentication.

### Technology Notes

- **Java 21**, Spring Boot 4.0.6
- **MapStruct** for all struct-to-struct mapping (configured with `defaultComponentModel=spring`; Lombok binding order matters — Lombok processor must come before MapStruct in `annotationProcessorPaths`)
- **H2** in dev (`spring.profiles.active=dev`), **PostgreSQL 16** in prod. Both JDBC drivers are declared in `bookland-app` (the assembly module), not in a domain module
- **Flyway owns the schema in both profiles**, and `ddl-auto` is `validate` in both (Flyway creates, Hibernate verifies the mapping and refuses to boot on a drift). Migrations live in `bookland-app/src/main/resources/db/migration`, versioned by **timestamp** (`V20260726164500__init_schema.sql`) so parallel branches cannot collide. Dev runs the same migrations against H2 opened with `MODE=PostgreSQL` — so **migration SQL must stay in the PostgreSQL/H2 common subset**, and a migration that breaks fails on the next dev boot rather than in prod. There is no `import.sql`; the categories are reference data in `V2`. **Boot 4 gotcha:** `flyway-core` alone does nothing — the auto-configuration lives in `spring-boot-flyway`, so the dependency must be `spring-boot-starter-flyway` (plus `flyway-database-postgresql`; H2 support is inside `flyway-core`). Without it Flyway fails silently: no error, no migrations applied
- **`AdminBootstrap` and `DevDataLoader` must both stay idempotent** — they look up before inserting (`GetUserByEmailUseCase`; `IsbnAlreadyExistsException` per book). The in-memory H2 survives a devtools restart (`DB_CLOSE_DELAY=-1`) and Flyway no longer wipes it as `create-drop` did, so a non-idempotent seed breaks the second start. The category UUIDs in `V2` are referenced literally by `DevDataLoader`'s constants — changing them breaks the book seed
- **Datasource URL is `${DB_URL:jdbc:postgresql://localhost:5432/bookland}`.** `docker-compose.yml` injects `DB_URL` pointing at the `postgres` service name; the default covers running the app from the host against the compose Postgres (port 5432 is published)
- **FKs exist only within a module.** Cross-module columns (`cart_items.book_id`, `orders.customer_id`, `payments.order_id`, …) are indexed `uuid` with no constraint, mirroring the absence of cross-module JPA relationships. Do not add them without discussing the module-split implications
- **Spring Authorization Server** (via `spring-boot-starter-security-oauth2-authorization-server`, versionless — it inherits `spring-security.version`). Issuer, RSA key pair, API audience and the one registered client are configured under `bookland.oauth2.*`; Flyway creates the three `oauth2_*` tables and `ClientBootstrap` writes the client row, idempotently. **Do not add JJWT back** — nothing signs tokens by hand any more
- **Admin bootstrap**: `AdminBootstrap` (bookland-app, `@Order(1)`, all profiles) guarantees exactly one admin user on startup — credentials under `bookland.admin.email/password` (prod: `ADMIN_EMAIL`/`ADMIN_PASSWORD` env vars). `DevDataLoader` (`@Order(2)`, dev only) seeds a sample customer and books
- **Cover image storage**: local filesystem in dev/single-node. Location under `bookland.storage.covers-location` (dev: `./bookland-data/covers`; prod: `STORAGE_COVERS_LOCATION` env, default `/var/bookland/covers` — mount a volume so uploads survive restarts). Upload limits under `spring.servlet.multipart.*` (5 MB); allowed types JPEG/PNG/WEBP validated in `UploadBookCoverService`
- **ArchUnit** (`archunit-junit5`, test scope) enforces framework-freedom of the inner layers and the inward dependency direction — one `ArchitectureRulesTest` per domain module
- **Tests** are plain JUnit 5 + Mockito + AssertJ unit tests against mocked ports (`TransactionPort` is faked with a pass-through, not mocked); `BooklandApplicationTests` (bookland-app, `@SpringBootTest`) boots the full context and validates all composition-root wiring. There are no `@WebMvcTest` slices
