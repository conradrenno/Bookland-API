# Bookland

> REST API for a complete e-commerce bookstore, built with **Java 21**, **Spring Boot 4** and strict **Clean Architecture** principles across 8 isolated domain modules.

---

## Table of Contents

- [About the Project](#about-the-project)
- [Architecture](#architecture)
  - [Module Structure](#module-structure)
  - [Layer Layout (per domain)](#layer-layout-per-domain)
  - [The Two Controllers](#the-two-controllers)
  - [Cross-domain Communication](#cross-domain-communication)
- [Design Patterns](#design-patterns)
- [Tech Stack](#tech-stack)
- [Domain Overview](#domain-overview)
- [API Reference](#api-reference)
- [Security Model](#security-model)
- [Database and Migrations](#database-and-migrations)
- [Running the Application](#running-the-application)
  - [Without Docker (dev)](#without-docker-dev)
  - [With Docker (prod)](#with-docker-prod)
- [Environment Variables](#environment-variables)
- [Testing](#testing)
- [Future Improvements](#future-improvements)

---

## About the Project

Bookland is a fully functional e-commerce API for an online bookstore. It covers the entire customer journey — from browsing the catalog and managing a wishlist, through checkout and payment processing, to leaving verified reviews.

The project was built with a deliberate focus on **software architecture and design**, using it as a vehicle to apply and validate enterprise-grade patterns in a real, runnable codebase. Every architectural decision — module isolation, port/adapter boundaries, use-case granularity — was made intentionally, not as boilerplate.

**What it covers:**
- User registration and JWT-based authentication (access + refresh token rotation)
- Book catalog with search, filtering, and category browsing
- Real-time stock management and inventory auditing
- Shopping cart with price snapshot at add time
- Checkout flow with integrated payment processing and transactional stock control
- Order lifecycle management with full status history and admin controls
- Verified reviews (only customers with a delivered order can review)
- Wishlist with direct move-to-cart capability
- Role-based access control (CUSTOMER / ADMIN) with automated admin bootstrap

---

## Architecture

### Module Structure

The project is a **multi-module Maven** project. Each domain is an independent module with its own dependencies, tests, and no awareness of sibling modules unless explicitly declared.

```
bookland/                       ← Parent POM (dependency management)
│
├── bookland-app/               ← Spring Boot entry point — no business logic
│                                 Assembles all domain modules; hosts application.yml
│
├── bookland-user/              ← User identity and profile management
├── bookland-auth/              ← JWT authentication and token lifecycle
├── bookland-catalog/           ← Book catalog, search, categories, stock quantity
├── bookland-inventory/         ← Stock movement ledger and low-stock observability
├── bookland-orders/            ← Shopping cart, checkout, order lifecycle
├── bookland-payments/          ← Payment processing (simulated gateway)
├── bookland-reviews/           ← Purchase-verified book reviews
└── bookland-wishlist/          ← Customer wishlist with move-to-cart
```

`bookland-app` has no business logic — it exists solely to assemble all domain modules into a single deployable artifact.

---

### Layer Layout (per domain)

Each domain module follows **four** layers, mapping onto Clean Architecture's concentric circles:

| Layer | Clean Architecture ring |
|---|---|
| `domain/` | Entities — enterprise business rules |
| `application/` | Use Cases — application business rules |
| `adapters/` | Interface Adapters — controllers, presenters |
| `infrastructure/` | Frameworks & Drivers |

Ports & Adapters is the **boundary mechanism** used throughout, not a competing style: `port/in` and `port/out` are how each ring is crossed.

Domain, Application and Adapters are **framework-free** — no Spring, no JPA, no Jackson. Only Infrastructure touches a framework. Lombok is allowed everywhere: it is source-only and leaves no bytecode trace.

```
com.devrenno.bookland.{domain}/
│
├── domain/                  [framework-free]
│   ├── entity/          ← Pure Java; static create/reconstitute factories,
│   │                       private constructor, no public setters
│   ├── valueobject/     ← Immutable value types (Email, UserId, ISBN...)
│   ├── service/         ← Domain rules needing lookup data — no I/O, no Spring
│   └── exception/       ← Domain-specific exceptions
│
├── application/             [framework-free]
│   ├── service/         ← Plain-Java *Service implementing port/in.
│   │                       Private constructor + static create(...) factory
│   ├── dto/             ← Input commands and query read-models
│   ├── common/          ← PageQuery / PageResult — framework-free pagination
│   └── port/
│       ├── in/          ← Use-case interfaces (CheckoutUseCase, RegisterUserUseCase)
│       └── out/         ← Outbound ports (persistence, transactions, cross-module)
│
├── adapters/                [framework-free]
│   ├── controller/      ← Internal controller — orchestrates use cases + presenter.
│   │                       Also the module's composition root
│   ├── presenter/       ← Domain entity → ViewModel
│   └── viewmodel/       ← Output DTOs — no Jackson annotations
│
└── infrastructure/          [Spring]
    ├── web/             ← @RestController, request DTOs, MapStruct mappers,
    │                       @RestControllerAdvice
    ├── config/          ← Composition-root @Beans calling *Controller.create(ports)
    ├── persistence/     ← JPA entities, Spring Data repos, persistence adapters
    ├── adapter/         ← Cross-module adapters implementing this module's out-ports
    ├── transaction/     ← TransactionAdapter implementing TransactionPort
    └── security/        ← JWT filter, BCrypt adapter (user/auth only)
```

**The dependency rule points inward and is enforced by tests:**

```
infrastructure → adapters → application → domain
```

Every module has an `ArchitectureRulesTest` (ArchUnit) that fails the build if an inner layer imports `org.springframework..`, `jakarta.persistence..` or `com.fasterxml.jackson..`, or if the layer direction is violated. There is **no `@UseCase` annotation** — inner classes are never Spring beans and never self-annotate.

---

### The Two Controllers

The name "controller" is used for two different things, deliberately:

| | **Internal controller** | **API controller** |
|---|---|---|
| Package | `adapters/controller` | `infrastructure/web` |
| Framework | None — plain Java | `@RestController` |
| Role | Orchestrates `port/in` use cases, calls the Presenter, returns a ViewModel | HTTP adapter: request → internal controller → `ResponseEntity<ViewModel>` |
| Extra role | **Composition root** — its static `create(ports)` wires domain service + use cases + presenter | — |
| Knows about | Use cases and the presenter | Only the internal controller — never a `*Service` |

Infrastructure creates only the outbound-port adapters (`@Repository` / `@Component`) and exposes **one `@Bean` per module entry point** that calls `*Controller.create(ports)`. A use case consumed by another module must also be exposed as its own `@Bean` — forgetting one fails context startup in the consumer.

**Use cases return domain entities**, not DTOs. Output shaping happens in the Presenter. The exception is a use case whose output needs data from another module: it returns a **query read-model** from `application/dto/`, assembled from the aggregate plus an out-port lookup.

> One deliberate deviation from canonical Clean Architecture: the use case *returns* its result and the internal controller then calls the Presenter, rather than the use case pushing through an output boundary into an injected presenter. In a synchronous HTTP context the output-port indirection buys nothing but ceremony, so it was dropped.

---

### Cross-domain Communication

Modules communicate exclusively through **use-case interfaces** — never by importing another module's services, repositories or JPA entities. A consumer depends on the source module's `port/in` and receives its **domain entities**, mapping them into its own types.

```
bookland-auth
    ├── UserLookupPort          → GetUserByEmailUseCase              (user)
    └── UserRegistrationPort    → RegisterUserUseCase                (user)

bookland-orders
    ├── BookInfoPort            → GetBookByIdUseCase                 (catalog)
    ├── BookStockPort           → AdjustBookStockUseCase             (catalog)
    ├── PaymentPort             → ProcessPaymentUseCase              (payments)
    └── RefundPort              → RefundPaymentUseCase               (payments)

bookland-inventory
    ├── BookStockAdjustmentPort → GetBookStockUseCase
    │                             + AdjustBookStockUseCase           (catalog)
    └── LowStockBooksPort       → GetLowStockBooksUseCase            (catalog)

bookland-reviews
    ├── PurchaseVerificationPort → VerifyPurchaseUseCase             (orders)
    ├── BookExistsPort           → GetBookByIdUseCase                (catalog)
    ├── BookRatingUpdatePort     → UpdateBookAverageRatingUseCase    (catalog)
    └── CustomerNamePort         → GetUserByIdUseCase                (user)

bookland-wishlist
    ├── CartAddPort             → AddCartItemUseCase                 (orders)
    └── WishlistBookInfoPort    → GetBookByIdUseCase                 (catalog)

bookland-catalog
    └── ActiveOrderCheckPort    → implemented by orders — a book cannot be
                                  removed while it sits in an active order
```

Note the last one: the adapter can live on either side. `ActiveOrderCheckPort` is declared by catalog and implemented in `bookland-orders`, inverting the dependency so catalog stays a leaf module.

---

## Design Patterns

| Pattern | Where Applied |
|---|---|
| **Ports & Adapters** | The boundary mechanism throughout — all I/O behind `port/in` and `port/out` interfaces |
| **The Dependency Rule** | `infrastructure → adapters → application → domain`, asserted by an ArchUnit `layeredArchitecture` rule per module |
| **Rich Domain Model** | Entities own their invariants: private constructors, `create`/`reconstitute` factories, no public setters |
| **Use Case per Class** | One `*Service` per use case (e.g. `CheckoutService`, `CancelOrderService`) |
| **Composition Root** | `*Controller.create(ports)` wires each module's graph by hand; no `@UseCase`, no self-annotating beans |
| **Presenter / ViewModel** | Use cases return domain entities; presenters shape them into Jackson-free ViewModels |
| **Value Object** | `Email`, `UserId`, `ISBN` — immutable, self-validating types |
| **Aggregate** | `Order` owns `OrderItem` and `StatusTransition`; `Cart` owns `CartItem` |
| **Repository Pattern** | All persistence behind `*PersistencePort` interfaces |
| **Adapter Pattern** | Cross-domain and infrastructure adapters implement out-ports |
| **Dependency Inversion across modules** | `ActiveOrderCheckPort` is declared by catalog and implemented by orders, keeping catalog a leaf |
| **Read Model (query-side DTO)** | `CartView`, `WishlistView`, `ReviewView`, `LowStockBook` — assembled in the application layer from the aggregate plus a cross-module lookup |
| **Graceful Degradation** | A cart or wishlist item whose book left the catalog renders as `"Unavailable"` / `available: false` instead of failing the whole response |
| **Framework-free Transactions** | `TransactionPort.inTransaction(Supplier<T>)`, implemented with `TransactionTemplate`; no `@Transactional` on application services |
| **Framework-free Pagination** | `PageQuery` / `PageResult<T>` per module; adapters translate to and from Spring's `PageRequest` / `Page` |
| **Domain Event (implicit)** | Status transitions recorded as `StatusTransition` history in `Order` |
| **Idempotent Bootstrap** | `AdminBootstrap` guarantees exactly one admin on every startup |
| **Soft Delete** | Books are deactivated, never deleted — invisible outside the catalog, still reachable by admin write flows |
| **Optimistic Price Snapshot** | Cart freezes unit price at add time; Order freezes price, title and cover at checkout |
| **Append-only Ledger** | `InventoryEntry` — insert-only, no updates, full audit trail |
| **Token Rotation** | Refresh tokens are single-use; each refresh issues a new pair |
| **Executable Architecture** | ArchUnit rules per module fail the build on a framework import in an inner layer |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.0.6 |
| Build | Apache Maven (multi-module) |
| Persistence | Spring Data JPA + Hibernate |
| Database (dev) | H2 (in-memory) |
| Database (prod) | PostgreSQL 16 |
| Schema migrations | Flyway 11 (`spring-boot-starter-flyway`) — owns the schema in dev and prod |
| File storage | Local filesystem behind `ImageStoragePort` (swappable for S3/GCS) |
| Authentication | JWT (JJWT 0.12.6) — HS256 |
| Object Mapping | MapStruct |
| Boilerplate reduction | Lombok |
| API Documentation | SpringDoc OpenAPI 3 (Swagger UI) |
| Testing | JUnit 5 + Mockito + AssertJ |
| Architecture testing | ArchUnit — one `ArchitectureRulesTest` per module |
| Containerization | Docker + Docker Compose |
| Code style | Conventional Commits |

---

## Domain Overview

### User
Manages customer identity and profile. Stores hashed passwords, name, email, role (`CUSTOMER` / `ADMIN`), and active status. Exposes use-case interfaces consumed by the Auth module.

### Auth
Handles the full JWT lifecycle: registration, login, access-token refresh, and logout. Issues short-lived **access tokens** (24h) and long-lived **refresh tokens** (7 days) with single-use rotation. The `JwtAuthenticationFilter` populates Spring Security's context on every request.

### Catalog
The source of truth for book data and stock quantity. Supports full-text search, filtering by category, price range, and average rating. Exposes stock adjustment and low-stock query use cases consumed by Inventory and Orders. ISBNs are normalised to their canonical 13-digit form on the way in.

Cover images are uploaded as `multipart/form-data` and stored through `ImageStoragePort`; the adapter writes the bytes to disk and returns a public `/media/covers/...` path. `MultipartFile` never crosses the web layer — the API controller extracts `byte[]` + filename + content type into a framework-free command.

**Book removal is a soft delete.** `GetBookByIdUseCase` — the in-port every other module reads books through — filters out inactive books, so a removed book cannot be fetched (404), added to a cart or wishlist (404), or checked out (409). Admin write flows bypass it and still see inactive books. On a `BookViewModel`, `available` means `active && stockQuantity > 0`.

### Inventory
An admin-facing audit ledger for manual stock adjustments. Records every delta with `previousQuantity`, `newQuantity`, `reason`, and `adjustedBy`. Does not store stock itself — that lives in Catalog. The low-stock endpoint enriches Catalog data with the timestamp of the last recorded manual movement.

### Orders
Manages the full purchase lifecycle:
- **Cart** — one per customer, with real-time stock validation and price snapshotting
- **Checkout** — validates stock, processes payment, decrements stock, and transitions the order atomically within a single transaction
- **Order lifecycle** — `AWAITING_PAYMENT → CONFIRMED → SHIPPED → DELIVERED` (or `CANCELLED` / `PAYMENT_FAILED`)
- **Cancellation** — from `AWAITING_PAYMENT` or `CONFIRMED`; the latter triggers stock restore and automatic refund

### Payments
Simulated payment gateway supporting `CREDIT_CARD`, `DEBIT_CARD`, `PAYPAL`, and `PIX`. Records payment status and provides refund capability. Consumed by Orders via outbound ports — Orders never accesses Payment internals directly.

### Reviews
Purchase-verified review system. Before creating a review, the service verifies (via `PurchaseVerificationPort → VerifyPurchaseUseCase` in Orders) that the customer has a `DELIVERED` order containing that book. On creation, the book's average rating in Catalog is recalculated automatically.

### Wishlist
Customer wishlist with atomic **move-to-cart** — removes the item from the wishlist and adds it to the cart in a single operation, reusing the cart's stock validation.

---

## API Reference

All endpoints are documented interactively at **`/swagger-ui.html`** when the application is running.

### Authentication — `/api/v1/auth`

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/register` | Public | Register and receive token pair |
| `POST` | `/login` | Public | Authenticate and receive token pair |
| `POST` | `/refresh` | Public | Rotate refresh token, get new access token |
| `POST` | `/logout` | Public | Revoke refresh token |

### Users — `/api/v1/users`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/{id}` | Authenticated | Get user profile |
| `PUT` | `/{id}` | Authenticated | Update user name |
| `DELETE` | `/{id}` | Authenticated | Deactivate account |

### Catalog — `/api/v1/books`, `/api/v1/categories`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/books` | Public | Search/filter books (q, category, price, sort, page) |
| `GET` | `/books/{bookId}` | Public | Get book details |
| `GET` | `/categories` | Public | List all categories |
| `GET` | `/categories/{categoryId}/books` | Public | List books by category |
| `POST` | `/books` | Admin | Create book |
| `PATCH` | `/books/{bookId}` | Admin | Update book |
| `POST` | `/books/{bookId}/cover` | Admin | Upload cover image (`multipart/form-data`, part `file`) |
| `DELETE` | `/books/{bookId}` | Admin | Remove book (soft delete) |

### Inventory — `/api/v1/books/{bookId}/inventory`, `/api/v1/inventory`

| Method | Path | Access | Description |
|---|---|---|---|
| `PATCH` | `/books/{bookId}/inventory` | Admin | Adjust stock with reason |
| `GET` | `/books/{bookId}/inventory/history` | Admin | Paginated adjustment history |
| `GET` | `/inventory/low-stock?threshold=5` | Admin | Books below stock threshold |

### Cart & Checkout — `/api/v1/cart`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/cart` | Authenticated | View cart |
| `POST` | `/cart/items` | Authenticated | Add item to cart |
| `PATCH` | `/cart/items/{bookId}` | Authenticated | Update item quantity |
| `DELETE` | `/cart/items/{bookId}` | Authenticated | Remove item |
| `POST` | `/cart/checkout` | Authenticated | Checkout (requires `paymentMethod`) |

### Orders — `/api/v1/orders`, `/api/v1/admin/orders`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/orders` | Authenticated | Order history (paginated) |
| `GET` | `/orders/{orderId}` | Authenticated | Get order details |
| `DELETE` | `/orders/{orderId}` | Authenticated | Cancel order |
| `GET` | `/admin/orders?status=&page=&size=` | Admin | All orders, newest first |
| `GET` | `/admin/orders/{orderId}` | Admin | Get any order's details |
| `GET` | `/admin/orders/customer/{customerId}` | Admin | Orders of a given customer |
| `PATCH` | `/admin/orders/{orderId}/status` | Admin | Update order status |

### Payments — `/api/v1/payments`, `/api/v1/admin/payments`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/payments/order/{orderId}` | Authenticated | Get payment record |
| `POST` | `/admin/payments/order/{orderId}/refund` | Admin | Issue manual refund |

### Reviews — `/api/v1/books/{bookId}/reviews`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/books/{bookId}/reviews` | Public | List reviews (paginated) |
| `POST` | `/books/{bookId}/reviews` | Authenticated | Submit review (purchase verified) |
| `DELETE` | `/books/{bookId}/reviews/{reviewId}` | Admin | Moderate (remove) review |

### Wishlist — `/api/v1/wishlist`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/wishlist` | Authenticated | View wishlist |
| `POST` | `/wishlist/items` | Authenticated | Add book to wishlist |
| `DELETE` | `/wishlist/items/{bookId}` | Authenticated | Remove from wishlist |
| `POST` | `/wishlist/items/{bookId}/move-to-cart` | Authenticated | Move item to cart |

---

## Security Model

- **Stateless JWT** — no server-side session; the filter validates the token on every request
- **Access token** — short-lived (24h default), carries `userId`, `email`, and `role` claims
- **Refresh token** — long-lived (7 days), single-use with rotation; stored in the database
- **Role-based access** — `CUSTOMER` for standard routes, `ADMIN` for management endpoints; enforced by Spring Security `hasRole()` rules in `SecurityConfig`
- **Admin bootstrap** — `AdminBootstrap` runs on every startup and idempotently ensures the configured admin account exists, driven by environment variables in production
- **Password hashing** — BCrypt via `PasswordEncoderPort` — infrastructure detail hidden behind an out-port

All authorization rules for every module live in a single `SecurityConfig`, inside `bookland-auth`. Rule order matters: specific admin routes are declared before broad `permitAll` patterns.

The filter stores the **userId in `Authentication.getDetails()`**; controllers read it via `extractUserId(Principal)` rather than trusting a path variable.

**Public routes:** `POST /api/v1/auth/**`, `GET /api/v1/books/**`, `GET /api/v1/categories/**`, `GET /media/**` (stored cover images), `/h2-console/**`, `/swagger-ui/**`, `/api-docs/**`. Everything else requires authentication; `/api/v1/admin/**` and all catalog/inventory writes require `ROLE_ADMIN`.

---

## Database and Migrations

**Flyway owns the schema in production.** Migrations live in `bookland-app/src/main/resources/db/migration` and run at startup, before Hibernate.

```
V20260726164500__init_schema.sql          ← 15 tables, FKs, indexes
V20260726164600__reference_categories.sql ← category reference data
```

Versions are **timestamps**, not sequential numbers, so parallel branches cannot collide on the same version.

`ddl-auto` stays on `validate` in prod — deliberately. Flyway creates the schema; Hibernate then verifies it matches the entity mapping and refuses to start if it does not. A migration forgotten after an entity change fails the boot instead of surfacing as a runtime error.

**Dev runs the same migrations.** H2 is opened in PostgreSQL compatibility mode (`MODE=PostgreSQL`) so it accepts the same SQL, which means every migration is exercised on every dev boot rather than being tried for the first time in production.

| | dev | prod |
|---|---|---|
| Database | H2 (in-memory, PostgreSQL mode) | PostgreSQL 16 |
| Schema owner | **Flyway** | **Flyway** |
| `ddl-auto` | `validate` | `validate` |
| Seed data | migration + `AdminBootstrap` + `DevDataLoader` | migration + `AdminBootstrap` |

Both bootstrap runners are **idempotent** — they check before inserting. This matters because the in-memory database survives a `spring-boot-devtools` restart (`DB_CLOSE_DELAY=-1` keeps it alive for the life of the JVM) and Flyway, unlike `create-drop`, does not wipe it.

> Foreign keys exist only **within** a module. Columns that reference another module (`cart_items.book_id`, `orders.customer_id`, `payments.order_id`, `refresh_tokens.user_id`, …) are indexed `uuid` values with no referential constraint — mirroring the absence of JPA relationships across module boundaries. Integrity is enforced in the application layer.

---

## Running the Application

### Without Docker (dev)

**Requirements:** Java 21, Maven 3.9+

```bash
# Clone the repository
git clone https://github.com/conradrenno/Bookland-API.git
cd bookland

# Run in dev profile (H2 in-memory database, seed data loaded automatically)
./mvnw spring-boot:run -pl bookland-app
```

The application starts on `http://localhost:8080`.

**Dev credentials (seeded automatically):**

| Role | Email | Password |
|---|---|---|
| Admin | admin@bookland.com | admin1234 |
| Customer | joao@bookland.com | joao1234 |

**Dev endpoints:**

| Tool | URL |
|---|---|
| Swagger UI | http://localhost:8080/swagger-ui.html |
| H2 Console | http://localhost:8080/h2-console |
| OpenAPI JSON | http://localhost:8080/api-docs |

> H2 Console JDBC URL: `jdbc:h2:mem:booklanddb`

---

### With Docker (prod)

**Requirements:** Docker, Docker Compose

```bash
# Copy and configure environment variables
cp .env.example .env   # edit with your values

# Build and start all services (app + PostgreSQL)
docker-compose up --build
```

The application starts on `http://localhost:8080` connected to a persistent PostgreSQL 16 instance. On a fresh volume, Flyway creates the whole schema on first boot — no manual setup.

Two volumes persist across restarts: `bookland-pgdata` (database) and `bookland-covers` (uploaded cover images).

To stop and wipe both volumes:

```bash
docker-compose down -v
```

---

## Environment Variables

Copy `.env.example` to `.env` and fill in the values before running with Docker.

| Variable | Required | Description |
|---|---|---|
| `POSTGRES_USER` | Prod | PostgreSQL username |
| `POSTGRES_PASSWORD` | Prod | PostgreSQL password |
| `JWT_SECRET` | Prod | Base64-encoded HMAC-SHA256 key (min 256 bits) |
| `JWT_EXPIRATION_MS` | Optional | Access token TTL in ms (default: 86400000 — 24h) |
| `JWT_REFRESH_EXPIRATION_MS` | Optional | Refresh token TTL in ms (default: 604800000 — 7d) |
| `ADMIN_EMAIL` | Prod | Bootstrap admin email |
| `ADMIN_PASSWORD` | Prod | Bootstrap admin password |
| `DB_URL` | Injected | JDBC URL. `docker-compose.yml` sets it to `jdbc:postgresql://postgres:5432/bookland` — the service name on the compose network. Not set in `.env`; the `application.yml` default (`localhost:5432`) covers running the app from the host |
| `STORAGE_COVERS_LOCATION` | Optional | Where cover images are written (default `/var/bookland/covers`). Mount a volume so uploads survive restarts |

Generate a secure JWT secret:
```bash
openssl rand -base64 64
```

---

## Testing

```bash
# Run all tests across all modules
./mvnw test

# Run tests for a specific module
./mvnw test -pl bookland-orders

# Run a single test class
./mvnw test -pl bookland-auth -Dtest=LoginServiceTest
```

The suite is **plain JUnit 5 + Mockito + AssertJ** against mocked ports — there are no `@WebMvcTest` slices and no database in the tests. `TransactionPort` is faked with a pass-through implementation rather than mocked.

Three kinds of test:
- **Unit tests** — domain services, application services and internal controllers, in isolation
- **Architecture tests** — one `ArchitectureRulesTest` per module (ArchUnit): fails the build if `domain`, `application` or `adapters` import Spring, JPA or Jackson, or if the inward dependency direction is broken
- **Context test** — `BooklandApplicationTests` boots the full Spring context, validating every composition root and cross-module `@Bean`

| Module | Test classes |
|---|---|
| user | `UserDomainServiceTest`, `RegisterUserServiceTest`, `UserControllerTest`, `ArchitectureRulesTest` |
| auth | `LoginServiceTest`, `RegisterServiceTest`, `RefreshAccessTokenServiceTest`, `LogoutServiceTest`, `AuthControllerTest`, `ArchitectureRulesTest` |
| catalog | `CreateBookServiceTest`, `GetBookByIdServiceTest`, `RemoveBookServiceTest`, `CatalogControllerTest`, `ISBNTest`, `ArchitectureRulesTest` |
| orders | `CheckoutServiceTest`, `CancelOrderServiceTest`, `CheckActiveOrdersServiceTest`, `GetCartServiceTest`, `ArchitectureRulesTest` |
| payments | `ProcessPaymentServiceTest`, `ArchitectureRulesTest` |
| reviews | `CreateReviewServiceTest`, `ArchitectureRulesTest` |
| inventory | `AdjustInventoryServiceTest`, `ArchitectureRulesTest` |
| wishlist | `AddWishlistItemServiceTest`, `ArchitectureRulesTest` |
| app | `BooklandApplicationTests` |

---

## Future Improvements

The current implementation intentionally keeps auth simple (direct JWT) to focus on domain architecture. The roadmap includes:

- **OAuth2 Authorization Code + PKCE** — replace the current JWT flow with a proper OAuth2 Authorization Server (Spring Authorization Server), moving toward a BFF (Backend for Frontend) pattern where the browser never touches tokens directly
- **Event-driven cross-domain communication** — replace in-process port calls with domain events via a message broker (e.g. Kafka or RabbitMQ), enabling true decoupling and eventual consistency between modules
- **Notification domain** — email/push notifications triggered by domain events (order confirmed, shipped, review approved)
- **Elasticsearch integration** — replace JPA-based book search with a dedicated search index for full-text, faceted, and relevance-ranked queries
- **Redis caching** — cache catalog reads and session-adjacent data (cart preview, token blocklist for logout)
- **Admin promotion endpoint** — `PATCH /api/v1/admin/users/{id}/role` to promote users without direct database access
- **CI/CD pipeline** — GitHub Actions workflow with test, build, Docker push, and deploy stages
- **Rate limiting** — per-IP and per-user throttling on auth and checkout endpoints

---

<div align="center">
  Built with care by <a href="https://github.com/conradrenno">conradrenno</a>
</div>
