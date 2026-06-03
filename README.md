# Bookland

> REST API for a complete e-commerce bookstore, built with **Java 21**, **Spring Boot 4** and strict **Clean Architecture** principles across 8 isolated domain modules.

---

## Table of Contents

- [About the Project](#about-the-project)
- [Architecture](#architecture)
  - [Module Structure](#module-structure)
  - [Layer Layout (per domain)](#layer-layout-per-domain)
  - [Cross-domain Communication](#cross-domain-communication)
- [Design Patterns](#design-patterns)
- [Tech Stack](#tech-stack)
- [Domain Overview](#domain-overview)
- [API Reference](#api-reference)
- [Security Model](#security-model)
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

Each domain module follows a strict **Clean Architecture / Hexagonal Architecture** layering:

```
com.devrenno.bookland.{domain}/
│
├── domain/
│   ├── entity/          ← Pure Java domain objects — zero framework annotations
│   ├── valueobject/     ← Immutable value types (Email, UserId, Token...)
│   ├── service/         ← Domain rules — no I/O, no Spring
│   └── exception/       ← Domain-specific exceptions
│
├── application/
│   ├── service/         ← @UseCase: implements use-case interfaces, orchestrates domain
│   ├── dto/             ← Commands and application-layer responses
│   └── port/
│       ├── in/          ← Use-case interfaces (e.g. CheckoutUseCase, RegisterUserUseCase)
│       └── out/         ← Outbound port interfaces (e.g. OrderPersistencePort, PaymentPort)
│
├── infrastructure/
│   ├── config/          ← Spring @Configuration — wires domain and application beans
│   ├── persistence/     ← JPA entities, Spring Data repositories, persistence adapters
│   └── adapter/         ← Implements out-ports: payment, stock, cross-domain calls
│
└── api/
    ├── controller/      ← @RestController — HTTP only; delegates to port/in interfaces
    ├── dto/             ← HTTP request/response DTOs
    └── mapper/          ← MapStruct mappers between API DTOs and application DTOs
```

**The dependency rule is strictly enforced:**
- `domain` depends on nothing
- `application` depends on `domain`
- `infrastructure` and `api` depend on `application` and `domain`
- Nothing depends on `infrastructure` or `api`

---

### Cross-domain Communication

Modules communicate exclusively through **use-case interfaces** — never by importing another module's services or repositories directly. This preserves encapsulation between bounded contexts.

```
bookland-auth
    └── UserLookupPort       → GetUserByEmailUseCase    (bookland-user)
    └── UserRegistrationPort → RegisterUserUseCase       (bookland-user)

bookland-orders
    └── BookInfoPort         → GetBookByIdUseCase        (bookland-catalog)
    └── BookStockPort        → AdjustBookStockUseCase    (bookland-catalog)
    └── PaymentPort          → ProcessPaymentUseCase     (bookland-payments)
    └── RefundPort           → RefundPaymentUseCase      (bookland-payments)

bookland-inventory
    └── BookStockAdjustmentPort → GetBookStockUseCase + AdjustBookStockUseCase (bookland-catalog)
    └── LowStockBooksPort    → GetLowStockBooksUseCase   (bookland-catalog)

bookland-reviews
    └── PurchaseVerificationPort → VerifyPurchaseUseCase (bookland-orders)
    └── RatingUpdatePort     → UpdateBookAverageRatingUseCase (bookland-catalog)

bookland-wishlist
    └── CartAddPort          → AddCartItemUseCase        (bookland-orders)
    └── BookValidationPort   → GetBookByIdUseCase        (bookland-catalog)
```

---

## Design Patterns

| Pattern | Where Applied |
|---|---|
| **Hexagonal Architecture (Ports & Adapters)** | Every domain — all I/O behind `port/in` and `port/out` interfaces |
| **Use Case per Class** | One `*Service` per use case (e.g. `CheckoutService`, `CancelOrderService`) |
| **Value Object** | `Email`, `UserId`, `Token` — immutable, self-validating types |
| **Aggregate** | `Order` owns `OrderItem` and `StatusTransition`; `Cart` owns `CartItem` |
| **Repository Pattern** | All persistence behind `*PersistencePort` interfaces |
| **Adapter Pattern** | Cross-domain and infrastructure adapters implement out-ports |
| **Domain Event (implicit)** | Status transitions recorded as `StatusTransition` history in `Order` |
| **Idempotent Bootstrap** | `AdminBootstrap` guarantees exactly one admin on every startup |
| **Optimistic Price Snapshot** | Cart freezes unit price at add time; Order freezes it again at checkout |
| **Append-only Ledger** | `InventoryEntry` — insert-only, no updates, full audit trail |
| **Token Rotation** | Refresh tokens are single-use; each refresh issues a new pair |

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
| Authentication | JWT (JJWT 0.12.6) — HS256 |
| Object Mapping | MapStruct |
| Boilerplate reduction | Lombok |
| API Documentation | SpringDoc OpenAPI 3 (Swagger UI) |
| Testing | JUnit 5 + Mockito + AssertJ |
| Test slices | `@WebMvcTest`, `@ExtendWith(MockitoExtension)` |
| Containerization | Docker + Docker Compose |
| Code style | Conventional Commits |

---

## Domain Overview

### User
Manages customer identity and profile. Stores hashed passwords, name, email, role (`CUSTOMER` / `ADMIN`), and active status. Exposes use-case interfaces consumed by the Auth module.

### Auth
Handles the full JWT lifecycle: registration, login, access-token refresh, and logout. Issues short-lived **access tokens** (24h) and long-lived **refresh tokens** (7 days) with single-use rotation. The `JwtAuthenticationFilter` populates Spring Security's context on every request.

### Catalog
The source of truth for book data and stock quantity. Supports full-text search, filtering by category, price range, and average rating. Exposes stock adjustment and low-stock query use cases consumed by Inventory and Orders.

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
| `GET` | `/books/{id}` | Public | Get book details |
| `GET` | `/categories` | Public | List all categories |
| `GET` | `/categories/{id}/books` | Public | List books by category |
| `POST` | `/books` | Admin | Create book |
| `PATCH` | `/books/{id}` | Admin | Update book |
| `DELETE` | `/books/{id}` | Admin | Remove book |

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
| `GET` | `/orders/{id}` | Authenticated | Get order details |
| `DELETE` | `/orders/{id}` | Authenticated | Cancel order |
| `GET` | `/admin/orders` | Admin | All orders (paginated) |
| `PATCH` | `/admin/orders/{id}/status` | Admin | Update order status |

### Payments — `/api/v1/payments`, `/api/v1/admin/payments`

| Method | Path | Access | Description |
|---|---|---|---|
| `GET` | `/payments/order/{orderId}` | Authenticated | Get payment record |
| `POST` | `/admin/payments/{orderId}/refund` | Admin | Issue manual refund |

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

The application starts on `http://localhost:8080` connected to a persistent PostgreSQL 16 instance.

To stop and remove volumes:

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

The test suite covers all domain modules with a combination of:
- **Unit tests** — application services tested in isolation with Mockito (`@ExtendWith(MockitoExtension.class)`)
- **Web layer tests** — controllers tested with `@WebMvcTest` slices
- **Integration test** — full Spring context startup validated on every CI-equivalent build

| Module | Test classes |
|---|---|
| user | `UserDomainServiceTest`, `RegisterUserServiceTest`, `UserControllerTest` |
| auth | `LoginServiceTest`, `RegisterServiceTest`, `RefreshAccessTokenServiceTest`, `LogoutServiceTest` |
| catalog | `CreateBookServiceTest`, `RemoveBookServiceTest`, `BookControllerTest` |
| orders | `CheckoutServiceTest`, `CancelOrderServiceTest` |
| reviews | `CreateReviewServiceTest` |
| inventory | `AdjustInventoryServiceTest` |
| wishlist | `AddWishlistItemServiceTest` |

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
