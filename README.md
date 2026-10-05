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
- [Error Contract](#error-contract)
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
- User registration and an embedded OAuth2/OIDC Authorization Server (authorization code + PKCE, RS256 tokens, single-use refresh rotation)
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
├── bookland-web-support/       ← Platform library — the HTTP error contract and
│                                 Bearer-token validation.
│                                 Not a domain; not a shared kernel
│
├── bookland-user/              ← User identity and profile management
├── bookland-auth/              ← OAuth2/OIDC Authorization Server + registration
├── bookland-catalog/           ← Book catalog, search, categories, stock quantity
├── bookland-inventory/         ← Stock movement ledger and low-stock observability
├── bookland-orders/            ← Shopping cart, checkout, order lifecycle
├── bookland-payments/          ← Payment processing (simulated gateway)
├── bookland-reviews/           ← Purchase-verified book reviews
└── bookland-wishlist/          ← Customer wishlist with move-to-cart
```

`bookland-app` has no business logic — it exists solely to assemble all domain modules into a single deployable artifact.

**`bookland-web-support` is the one module every other module depends on**, and the "duplicate it per module" rule does not apply to it. It holds the glue that renders the HTTP error contract — `ProblemDetails`, `ProblemDetailWriter`, `ProblemDetailErrorController`, the Spring Security entry points, the single bean-validation advice, the OpenAPI error-response customizer — and the resource-server half of security (`ResourceServerConfig`: the `JwtDecoder` and its validators), so that a service can validate a token without depending on `bookland-auth` and its private key. It exists because that contract has to be **byte-identical across all 8 domains**: duplicated, the shape drifts — one module emitting a `code`, another not; one answering in English, another in whatever language the JVM defaults to.

It is not a shared kernel. Anything with domain meaning is still duplicated per module (`PageQuery`, `PageResult`). This module may never contain a domain type or depend on another `bookland-*` module, and **only `*.infrastructure` packages may import it** — a rule the ArchUnit suite enforces by listing `com.devrenno.bookland.websupport..` next to `org.springframework..` in the framework packages banned from the inner layers. To domain, application and adapters, it *is* a framework. It is packaged separately so the error contract survives a future split into independently deployed services.

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
    ├── messaging/       ← Kafka producers (implementing out-ports) and @KafkaListener inbound adapters
    └── security/        ← Token customizer and validators, UserDetails, BCrypt adapter (user/auth only)
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
    ├── UserLookupPort          → GetUserByEmailUseCase
    │                             + GetUserByIdUseCase               (user)
    └── UserRegistrationPort    → RegisterUserUseCase                (user)

bookland-orders
    ├── BookInfoPort            → GetBookByIdUseCase                 (catalog)
    ├── BookStockPort           → DecrementBookStockUseCase
    │                             + IncrementBookStockUseCase        (catalog)
    ├── PaymentPort             → ProcessPaymentUseCase              (payments)
    └── RefundPort              → RefundPaymentUseCase               (payments)

bookland-inventory
    ├── BookStockAdjustmentPort → AdjustBookStockUseCase             (catalog)
    └── LowStockBooksPort       → GetLowStockBooksUseCase            (catalog)

bookland-reviews
    ├── PurchaseVerificationPort → VerifyPurchaseUseCase             (orders)
    ├── BookExistsPort           → GetBookByIdUseCase                (catalog)
    ├── BookRatingEventPort      → Kafka topic bookland.reviews.book-rating-changed
    │                              consumed by catalog's BookRatingChangedListener
    └── CustomerNamePort         → GetUserByIdUseCase                (user)
                                   called once, at creation; the name is stored on the review

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
| **Read Model (query-side DTO)** | `CartView`, `WishlistView`, `LowStockBook` — assembled in the application layer from the aggregate plus a cross-module lookup |
| **Graceful Degradation** | A cart or wishlist item whose book left the catalog renders as `"Unavailable"` / `available: false` instead of failing the whole response |
| **Framework-free Transactions** | `TransactionPort.inTransaction(Supplier<T>)`, implemented with `TransactionTemplate`; no `@Transactional` on application services |
| **Framework-free Pagination** | `PageQuery` / `PageResult<T>` per module; adapters translate to and from Spring's `PageRequest` / `Page` |
| **Domain Event (implicit)** | Status transitions recorded as `StatusTransition` history in `Order` |
| **Idempotent Bootstrap** | `AdminBootstrap` guarantees exactly one admin on every startup |
| **Soft Delete** | Books are deactivated, never deleted — invisible outside the catalog, still reachable by admin write flows. User accounts likewise: `DELETE` deactivates, and the e-mail stays taken |
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
| Authentication | Spring Authorization Server (OAuth2 + OIDC) — RS256; every module is a Resource Server |
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
Manages customer identity and profile. Stores hashed passwords, name, email, role (`CUSTOMER` / `ADMIN`), and active status. Exposes use-case interfaces consumed by the Auth and Reviews modules.

**Deleting an account deactivates it.** The row stays, so the e-mail can never be registered again by someone else; to every lookup by id the account is gone (404), the login refuses it, and a refresh token issued before the deletion answers `invalid_grant`. An admin account cannot be deleted (409), since `AdminBootstrap` would otherwise find it deactivated and leave the system without an admin.

### Auth
Hosts an **OAuth2 Authorization Server with OIDC** (Spring Authorization Server). Login is `authorization_code` + PKCE at `/oauth2/authorize` and `/oauth2/token`, which issue an access token (15 minutes), an `id_token` and a refresh token (7 days, single-use rotation). Every other module is a Resource Server, verifying those tokens against the public key published at `/oauth2/jwks`. Registration stays in this module as `POST /api/v1/auth/register`, because it is business logic rather than authentication.

### Catalog
The source of truth for book data and stock quantity. Supports full-text search, filtering by category, price range, and average rating. Exposes stock adjustment and low-stock query use cases consumed by Inventory and Orders. ISBNs are normalised to their canonical 13-digit form on the way in.

Cover images are uploaded as `multipart/form-data` and stored through `ImageStoragePort`; the adapter writes the bytes to disk and returns a public `/media/covers/...` path. `MultipartFile` never crosses the web layer — the API controller extracts `byte[]` + filename + content type into a framework-free command.

**Book removal is a soft delete.** `GetBookByIdUseCase` — the in-port every other module reads books through — filters out inactive books, so a removed book cannot be fetched (404), added to a cart or wishlist (404), or checked out (409). Admin write flows bypass it and still see inactive books. On a `BookViewModel`, `available` means `active && stockQuantity > 0`.

### Inventory
An admin-facing audit ledger for manual stock adjustments. Records every delta with `previousQuantity`, `newQuantity`, `reason`, and `adjustedBy`. Does not store stock itself — that lives in Catalog. The low-stock endpoint enriches Catalog data with the timestamp of the last recorded manual movement.

### Orders
Manages the full purchase lifecycle:
- **Cart** — one per customer, with real-time stock validation and price snapshotting
- **Checkout** — validates stock, processes payment, decrements stock, and transitions the order in a single transaction: either all four land or none do
- **Order lifecycle** — `AWAITING_PAYMENT → CONFIRMED → SHIPPED → DELIVERED` (or `CANCELLED` / `PAYMENT_FAILED`)
- **Cancellation** — from `AWAITING_PAYMENT` or `CONFIRMED`; the latter triggers stock restore and automatic refund

#### Stock under concurrency

A single transaction buys atomicity, not isolation — the two are separate guarantees and only the first one follows from wrapping the work in a transaction. Stock therefore moves only through **relative UPDATEs** evaluated by the database, never through a read-modify-write in Java.

Checkout consumes units with a conditional decrement:

```sql
UPDATE books SET stock_quantity = stock_quantity - :quantity
 WHERE id = :id AND active = true AND stock_quantity >= :quantity
```

`BookPersistencePort.tryDecrementSellableStock` returns whether that statement matched a row. Zero rows means the units are gone, which `CheckoutService` reports as `CartItemUnavailableException` (409) rather than confirming an order the catalog cannot fulfil.

Cancellation returns them with the mirror statement, deliberately **without** the `active` filter — a delisted book must not be *sold*, but units coming back from a cancelled order are still units, and dropping them would leave the count wrong for good if the book is ever relisted:

```sql
UPDATE books SET stock_quantity = stock_quantity + :quantity WHERE id = :id
```

The asymmetry in the return types follows the same logic. A decrement has a guard that can legitimately fail, so it answers `boolean`; an increment has none, so it only reports whether the book existed at all, and `IncrementBookStockService` turns a miss into `BookNotFoundException` rather than discarding the units silently.

**Three operations, not two.** Inventory's admin correction carries a *signed* delta, so `AdjustBookStockService` routes it to whichever relative UPDATE matches the sign. Its decrement is a third statement — the same guard against going negative, but again without the `active` filter, because admin write flows deliberately still reach delisted books and refusing to correct their count would strand it. Only selling requires the book to be active. The signature and the errors are unchanged from the read-modify-write version it replaces, `InsufficientStockException` (422) included, so nothing downstream noticed.

`AdjustInventoryService` also gained a `TransactionPort`, and both halves of that change matter:

- **The ledger and the stock now commit together.** Before, a failure to save the audit entry left the stock already changed and unrecorded.
- **`previousQuantity` is derived, not read.** It used to call `getCurrentStock` and *then* adjust — two statements with a window between them, so the recorded "previous" could be a value this adjustment never started from, and the audit trail disagreed with the stock it existed to explain. Subtracting the delta from the result is exact, because the adjustment applied that delta atomically and the row lock it took is still held. `getCurrentStock` was removed from `BookStockAdjustmentPort` entirely — leaving it there is an invitation to reintroduce the bug.

**Why not the alternatives.** The obvious read-modify-write — load the book, subtract in Java, save — is what this replaced, and it cannot hold the invariant no matter how the transaction is configured: two checkouts both read `stock=1`, both compute `0`, and both write the absolute value `0`. One unit is sold twice, silently, because a domain guard on the entity only ever sees the snapshot its own transaction read. A row lock does not save it either, since the value written is a constant computed before the lock was taken. `@Version` (optimistic locking) would protect every write to `books` rather than just this one, but it surfaces the conflict as an exception *after* the payment is approved, forcing a retry that re-charges or a compensating refund — and it turns a popular title into a retry storm. `PESSIMISTIC_WRITE` is correct but would hold the row lock across the payment gateway call, which sits inside the same transaction. The conditional `UPDATE` needs no version column, no migration, no retry, and no lock held over network I/O.

**Where the guard is enforced.** In the SQL predicates, and only there. `Book` deliberately exposes **no method to move stock** — the old `Book.adjustStock`, which computed `stockQuantity + delta` in memory and threw when the result went negative, was deleted rather than left as dead code: it read like the safe way to change stock and was the exact read-modify-write this section is about, so keeping it around was a trap regardless of any warning attached to it. The invariant is now stated once, by the party that can evaluate it in the same statement that writes.

The one absolute write left is `Book.update`, the admin edit that *sets* a stock quantity outright rather than moving it. It overwrites by intent — the admin is asserting a count, not a change — so it has no relative form and needs no guard.

**Why three in-ports.** `DecrementBookStockUseCase` and `IncrementBookStockUseCase` are separate from `AdjustBookStockUseCase` because Orders never applies a signed adjustment — it consumes or returns a known number of units, and running out of stock mid-race is an expected outcome rather than an error, so it answers `boolean` instead of throwing. Inventory is the opposite: an admin submits a delta whose direction the caller does not know in advance, and a correction that would go negative is a genuine mistake worth a 422.

**What this does not fix.** The stock reading that validates the cart is still a separate statement from the decrement, so a checkout can pass validation and then lose the race. It now *fails* instead of overselling — but it fails after the payment was approved, and the rollback that undoes the charge is only safe because the gateway is simulated. A real PSP would need a compensating refund. Closing that properly means reserving stock before charging rather than after; see **Future Improvements**.

All of it is pinned by `StockConcurrencyIntegrationTest` (bookland-app), which races twenty checkouts for five copies, twenty cancellations returning a unit each, and twenty admin corrections of +1, all against the real database — a mocked persistence port would have passed against the broken implementation.

### Payments
Simulated payment gateway supporting `CREDIT_CARD`, `DEBIT_CARD`, `PAYPAL`, and `PIX`. Records payment status and provides refund capability. Consumed by Orders via outbound ports — Orders never accesses Payment internals directly.

### Reviews
Purchase-verified review system. Before creating a review, the service verifies (via `PurchaseVerificationPort → VerifyPurchaseUseCase` in Orders) that the customer has a `DELIVERED` order containing that book. On creation and on moderation, the book's new average rating is published as a `BookRatingChanged` Kafka event that Catalog consumes — so the rating updates a moment after the response, not within it — and the author's display name is stored on the review — so listing reviews never asks the User module, and a review keeps the name its author had when writing it.

### Wishlist
Customer wishlist with atomic **move-to-cart** — removes the item from the wishlist and adds it to the cart in a single operation, reusing the cart's stock validation.

---

## API Reference

All endpoints are documented interactively at **`/swagger-ui.html`** when the application is running.

**Every date on the wire is an instant in UTC** — ISO-8601 ending in `Z` (`2026-08-05T18:17:49.755549Z`), never a local date-time. A client parses it with `Instant.parse` / `new Date(...)` and renders it in the viewer's own zone; no field anywhere in the API requires the reader to guess which zone it was written in. Columns are `timestamptz`, entities hold `Instant`, and `TimestampRulesTest` fails the build on a surviving `LocalDateTime`.

### Authentication — `/api/v1/auth`

| Method | Path | Access | Description |
|---|---|---|---|
| `POST` | `/register` | Public | Register — answers 201 with the account (`id`, `email`, `role`), no token, and signs the caller in to the Authorization Server |

There is no login, refresh or logout endpoint under `/api/v1/auth`. Those are protocol endpoints of the Authorization Server:

| Endpoint | Purpose |
|---|---|
| `GET /oauth2/authorize` | Start the authorization code flow (PKCE required); redirects to `/login` when there is no session |
| `POST /oauth2/token` | Exchange the code (`grant_type=authorization_code`) or refresh (`grant_type=refresh_token`, single-use) |
| `GET /oauth2/jwks` | Public key that verifies the tokens |
| `GET /userinfo` | OIDC claims of the caller |
| `GET /connect/logout` | End the Authorization Server session |
| `GET /.well-known/openid-configuration` | Discovery document |

### Users — `/api/v1/users`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/{id}` | Authenticated | Get user profile |
| `PUT` | `/{id}` | Authenticated | Update user name |
| `DELETE` | `/{id}` | Authenticated | Deactivate own account — login and refresh stop working, the e-mail stays taken; an admin account answers 409 |

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
| `GET` | `/orders?page=&size=` | Authenticated | Order history, newest first |
| `GET` | `/orders/{orderId}` | Authenticated | Get order details |
| `DELETE` | `/orders/{orderId}` | Authenticated | Cancel order |
| `GET` | `/admin/orders?status=&page=&size=` | Admin | All orders, newest first |
| `GET` | `/admin/orders/{orderId}` | Admin | Get any order's details |
| `GET` | `/admin/orders/customer/{customerId}?page=&size=` | Admin | Orders of a given customer, newest first |
| `PATCH` | `/admin/orders/{orderId}/status` | Admin | Update order status |

**Order listings are not client-sortable.** Every route above is served newest
first (`createdAt` descending, ties broken by `id` so paging cannot drop or
repeat a row); the only pagination parameters are `page` and `size`. There is no
`sort` parameter — a request carrying one is answered normally with the standard
order, not rejected, because unknown query parameters are ignored API-wide.

### Payments — `/api/v1/payments`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/payments/order/{orderId}` | Owner | Get the payment for your own order |

There is no refund endpoint. A refund is one half of a cancellation — issuing it on its own left
the order `CONFIRMED` and the stock never returned, which is the mirror of the admin-cancellation
bug fixed earlier. Refunding is reached through `PATCH /admin/orders/{orderId}/status` → `CANCELLED`,
which compensates stock and payment together via `OrderCancellation`.

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

- **Authorization Server + Resource Server** — `bookland-auth` issues the tokens; the API validates them as a resource server, statelessly, against the public key
- **Access token** — short-lived (15 min default), RS256, `aud = bookland-api`; carries `sub` (the user id, never the e-mail), `email` and `role`
- **`id_token`** — addressed to the client, not the API; refused as a Bearer credential
- **Refresh token** — long-lived (7 days), single-use with rotation, stored in `oauth2_authorization`. A refresh looks the account up again: a deleted account is `invalid_grant`, and the new token carries the role the account has now
- **Role-based access** — `CUSTOMER` for standard routes, `ADMIN` for management endpoints; the `role` claim becomes a `ROLE_*` authority checked by `hasRole()` rules in `SecurityConfig`
- **Admin bootstrap** — `AdminBootstrap` runs on every startup and idempotently ensures the configured admin account exists, driven by environment variables in production
- **Password hashing** — BCrypt; registration hashes through `PasswordEncoderPort`, and the login checks the password through Spring's `DaoAuthenticationProvider`

**Each module declares its own access rules**, next to the controllers they protect, as an `AuthorizationRules` bean — so a module extracted into a service takes its rules with it. A module lists only what departs from the default (its public routes, and admin routes outside `/api/v1/admin`); `SecurityConfig` applies them all, then the default: the whole `/api/v1/admin/**` prefix is admin-only, so a new back-office controller is closed by default, and everything else requires authentication. Rules of different modules are written so they can never match the same request, which makes their order irrelevant. `AccessMatrixIntegrationTest` pins who may call every route and fails the build on a new route nobody classified.

A handler that needs the caller declares an **`AuthenticatedUser`** parameter, resolved from the token's `sub` — never a path variable, `Principal` or `SecurityContextHolder`.

**Known limitation — ending a session does not invalidate the access token.** `/connect/logout` (OIDC) ends the Authorization Server session and the refresh token stops working, so the session cannot be extended past the current access token. But the access token is stateless: nothing is looked up when it is validated, so it keeps working until it expires. A user who signed out stays authenticable for up to the access-token TTL — which is why that TTL is 15 minutes and not hours. This is the standard trade-off of stateless JWT, and the standard mitigation is exactly this: keep the access token short and let rotation do the rest. Making logout immediate requires server-side state on every request — a revocation list keyed by token id, listed under [Future Improvements](#future-improvements) as part of the Redis item.

**Public routes:** `POST /api/v1/auth/register`, the Authorization Server's own endpoints (`/oauth2/**`, `/login`, `/.well-known/**`), `GET /api/v1/books/**`, `GET /api/v1/categories/**`, `GET /media/**` (stored cover images), `/error`, `/h2-console/**`, `/swagger-ui/**`, `/api-docs/**`. Everything else requires authentication; `/api/v1/admin/**` and all catalog/inventory writes require `ROLE_ADMIN`.

Note that "public" no longer means a bad token is ignored. The resource server refuses an unusable Bearer token wherever one is presented, so `GET /api/v1/books` with an expired token answers 401 rather than serving the catalogue. The exceptions are the routes that are not the API at all — `/error`, `/media/**`, the console and the API document — which sit on a chain without a resource server precisely so that a stale token cannot turn a 500 into a 401.

**`/error` is public on purpose and must stay that way.** Boot registers the security chain for the `ERROR` dispatch too, so when an unhandled exception makes the container forward to `/error`, an authenticated `/error` answers the *forward* with `401 TOKEN_MISSING`. The real 500 never reaches the client — it arrives disguised as an expired session, which makes the client refresh its token and then log the user out over a server-side bug.

---

## Error Contract

> Full reference: **[`docs/error-contract.md`](docs/error-contract.md)** — every code, every status, and how a client should react to each.

Every error response in the API is `application/problem+json` ([RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)), in English, carrying one extension member on top of the standard ones:

```json
{
  "detail": "The access token has expired",
  "instance": "/api/v1/cart",
  "status": 401,
  "title": "Unauthorized",
  "code": "TOKEN_EXPIRED"
}
```

**`code` is the contract; `detail` is not.** `detail` is prose meant for a banner and may be reworded at any time — a client branches on `code`, which only changes with a breaking release.

**401 and 403 mean different things and are never conflated.** A 401 says the credential is missing or no longer good (`TOKEN_MISSING`, `TOKEN_EXPIRED`, `TOKEN_INVALID`) — refresh, then retry. A 403 says the credential is fine but the role is not (`INSUFFICIENT_ROLE`) — refreshing is pointless. And **not every 403 is about a role**: `ORDER_ACCESS_DENIED` and `PURCHASE_REQUIRED` are business 403s that say nothing about the caller's authorities, which is exactly why the status alone is not enough to branch on.

**A rejected payload carries an `errors` map** — field name → the messages that field broke, always as arrays — so each message renders next to its own input:

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "Validation failed for 2 fields: email, password",
  "errors": {
    "email": ["must be a well-formed email address"],
    "password": ["must contain at least one number", "size must be between 8 and 72"]
  }
}
```

Messages never name their own field (the key already does) and are always English, whatever the server's locale or the request's `Accept-Language`.

**Business rules are not validation errors** — they carry `detail`, no `errors` map, and a code owned by the module that owns the rule (`ISBN_ALREADY_EXISTS`, `INSUFFICIENT_STOCK`, `PAYMENT_DECLINED`, …). **A 5xx never echoes the exception message**: `detail` is always `"The server failed to process the request"`, because the exception's own text carries stack traces, SQL and column names. The cause goes to the log, never to the client.

The contract is published in `GET /api-docs` — a `ProblemDetail` schema, a `ValidationProblemDetail` schema, a `default` error response on every operation and an explicit `400` wherever a request takes input — so a client generates its error type rather than hand-writing it.

None of this is documentation-only. `AuthErrorContractIntegrationTest`, `BusinessErrorContractIntegrationTest`, `ValidationErrorContractIntegrationTest` and `OpenApiErrorContractIntegrationTest` lock each half of it against the running application; the glue itself lives in [`bookland-web-support`](#module-structure), out of reach of every inner layer.

---

## Database and Migrations

**Flyway owns the schema in both profiles.** Migrations live in `bookland-app/src/main/resources/db/migration` and run at startup, before Hibernate.

```
V20260726164500__init_schema.sql            ← 15 tables, FKs, indexes
V20260726164600__reference_categories.sql   ← category reference data
V20260730120000__timestamps_with_time_zone.sql
                                            ← every timestamp column → timestamptz
V20260810093000__oauth2_authorization_server_schema.sql
                                            ← the Authorization Server's three oauth2_* tables
V20260810210000__drop_refresh_tokens.sql    ← the hand-rolled refresh token table, retired
V20261003120000__reviews_customer_name.sql  ← author name stored on the review, backfilled from users
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

> Foreign keys exist only **within** a module. Columns that reference another module (`cart_items.book_id`, `orders.customer_id`, `payments.order_id`, …) are indexed `uuid` values with no referential constraint — mirroring the absence of JPA relationships across module boundaries. Integrity is enforced in the application layer.

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

> To use Swagger's **Authorize** button, open it at `http://127.0.0.1:8080/swagger-ui.html`, not `localhost`: the Authorization Server rejects `localhost` redirect URIs (RFC 8252). The dialog asks for the client id and secret (`bookland-web` / `bookland-web-secret` in dev), then sends you through the login page.

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
| `OAUTH2_ISSUER` | Prod | The URL clients actually reach the server on. Published in the discovery document and written into the `iss` claim; a mismatch is only noticed at validation time |
| `OAUTH2_JWK_PRIVATE_KEY` | Prod | RSA private key, base64 of the PKCS#8 DER, single-line. **The secret of the whole system** — whoever holds it mints admin tokens |
| `OAUTH2_JWK_PUBLIC_KEY` | Prod | RSA public key, base64 of the X.509 DER. Published at `/oauth2/jwks`; publishing it is the point |
| `OAUTH2_CLIENT_ID` | Prod | Client id of the one registered client |
| `OAUTH2_CLIENT_SECRET` | Prod | Its secret, in plain text — `ClientBootstrap` BCrypts it before it reaches the table |
| `OAUTH2_CLIENT_REDIRECT_URIS` | Prod | Comma-separated. Must be loopback IPs rather than `localhost`, which the server rejects (RFC 8252) |
| `OAUTH2_ACCESS_TOKEN_TTL_MINUTES` | Optional | Access token TTL (default: 15). Raising it widens the window after sign-out — see [Security Model](#security-model) |
| `OAUTH2_REFRESH_TOKEN_TTL_DAYS` | Optional | Refresh token TTL (default: 7) |
| `ADMIN_EMAIL` | Prod | Bootstrap admin email |
| `ADMIN_PASSWORD` | Prod | Bootstrap admin password |
| `DB_URL` | Injected | JDBC URL. `docker-compose.yml` sets it to `jdbc:postgresql://postgres:5432/bookland` — the service name on the compose network. Not set in `.env`; the `application.yml` default (`localhost:5432`) covers running the app from the host |
| `STORAGE_COVERS_LOCATION` | Optional | Where cover images are written (default `/var/bookland/covers`). Mount a volume so uploads survive restarts |

Generate the RSA key pair (base64 of the DER, single-line):
```bash
openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:2048 -outform DER -out k.der
openssl pkcs8 -topk8 -nocrypt -inform DER -in k.der -outform DER -out private.der
openssl rsa -in k.der -inform DER -pubout -outform DER -out public.der
openssl base64 -A -in private.der    # OAUTH2_JWK_PRIVATE_KEY
openssl base64 -A -in public.der     # OAUTH2_JWK_PUBLIC_KEY
```

---

## Testing

```bash
# Run all tests across all modules
./mvnw test

# Run tests for a specific module
./mvnw test -pl bookland-orders

# Run a single test class
./mvnw test -pl bookland-auth -Dtest=BooklandTokenCustomizerTest
```

**Inside a domain module** the tests are plain JUnit 5 + Mockito + AssertJ against mocked ports — no Spring context, no database, no `@WebMvcTest` slices. `TransactionPort` is faked with a pass-through implementation rather than mocked. That is what the manual composition root buys: a use case is constructed with `new`, so testing it needs no framework.

**The contract tests live in `bookland-app`** — the only module with every other module on the classpath, and therefore the only place the real filter chain, the real advices and the real database exist at once. These do boot Spring (`@SpringBootTest`) and do hit H2.

Four kinds of test:
- **Unit tests** — domain services, application services and internal controllers, in isolation
- **Architecture tests** — one `ArchitectureRulesTest` per module (ArchUnit): fails the build if `domain`, `application` or `adapters` import Spring, JPA, Jackson or `bookland-web-support`, or if the inward dependency direction is broken. Two more live in `bookland-app`, where the whole classpath is visible: `WebLayerRulesTest` fails a handler that returns a non-200 without `@ResponseStatus` (springdoc would publish the wrong status), and `TimestampRulesTest` fails any surviving `LocalDateTime` field
- **Contract tests** — `@SpringBootTest` against the assembled application: they pin what a client actually receives (error bodies, status codes, the published OpenAPI document, date formats, ordering) rather than what a mock was told to return
- **Context test** — `BooklandApplicationTests` boots the full Spring context, validating every composition root and cross-module `@Bean`

| Module | Test classes |
|---|---|
| user | `UserDomainServiceTest`, `RegisterUserServiceTest`, `UserControllerTest`, `ArchitectureRulesTest` |
| auth | `RegisterServiceTest`, `AuthControllerTest`, `BooklandTokenCustomizerTest`, `ArchitectureRulesTest` |
| catalog | `CreateBookServiceTest`, `GetBookByIdServiceTest`, `RemoveBookServiceTest`, `AdjustBookStockServiceTest`, `DecrementBookStockServiceTest`, `IncrementBookStockServiceTest`, `CatalogControllerTest`, `ISBNTest`, `ArchitectureRulesTest` |
| orders | `CheckoutServiceTest`, `CancelOrderServiceTest`, `UpdateOrderStatusServiceTest`, `CheckActiveOrdersServiceTest`, `GetCartServiceTest`, `ArchitectureRulesTest` |
| payments | `ProcessPaymentServiceTest`, `GetPaymentByOrderIdServiceTest`, `ArchitectureRulesTest` |
| reviews | `CreateReviewServiceTest`, `ListReviewsServiceTest`, `ArchitectureRulesTest` |
| inventory | `AdjustInventoryServiceTest`, `ArchitectureRulesTest` |
| wishlist | `AddWishlistItemServiceTest`, `ArchitectureRulesTest` |
| app | `BooklandApplicationTests`, `AuthorizationCodeFlowIntegrationTest`, `AuthErrorContractIntegrationTest`, `AuthenticatedUserArgumentResolverTest`, `BusinessErrorContractIntegrationTest`, `ValidationErrorContractIntegrationTest`, `OpenApiErrorContractIntegrationTest`, `TimestampContractIntegrationTest`, `OrderHistoryOrderingIntegrationTest`, `ReviewAuthorNameIntegrationTest`, `AccessMatrixIntegrationTest`, `StockConcurrencyIntegrationTest`, `ProblemDetailErrorControllerTest`, `WebLayerRulesTest`, `TimestampRulesTest` |
| web-support | — (exercised entirely through the app's contract tests) |

---

## Future Improvements

The roadmap includes:

- **BFF (Backend for Frontend)** — the Authorization Server is in place; the next step on the client side is a BFF that holds the tokens server-side, so the browser never touches them
- **Event-driven cross-domain communication** — replace in-process port calls with domain events via a message broker (e.g. Kafka or RabbitMQ), enabling true decoupling and eventual consistency between modules
- **Notification domain** — email/push notifications triggered by domain events (order confirmed, shipped, review approved)
- **Elasticsearch integration** — replace JPA-based book search with a dedicated search index for full-text, faceted, and relevance-ranked queries
- **Redis caching** — cache catalog reads and session-adjacent data (cart preview). Also the natural home for an **access-token revocation list**, which is what would make logout immediate instead of bounded by the 15-minute TTL (see [Security Model](#security-model)) — the trade is a lookup on every authenticated request, so it buys immediacy at the cost of the statelessness that makes the filter free today
- **Admin promotion endpoint** — `PATCH /api/v1/admin/users/{id}/role` to promote users without direct database access
- **CI/CD pipeline** — GitHub Actions workflow with test, build, Docker push, and deploy stages
- **Rate limiting** — per-IP and per-user throttling on auth and checkout endpoints
- **Stock reservation at checkout** — reserve units *before* charging and release them on failure or expiry, instead of decrementing after the payment is approved. Today a checkout that loses the race for the last copies fails cleanly (see [Stock under concurrency](#stock-under-concurrency)), but it fails with the payment already approved, which only rolls back safely because the gateway is simulated. A reservation with a TTL trades "sold what we did not have" for "held what we did not sell" — the cheaper of the two errors — and is the prerequisite for plugging in a real payment provider

---

<div align="center">
  Built with care by <a href="https://github.com/conradrenno">conradrenno</a>
</div>
