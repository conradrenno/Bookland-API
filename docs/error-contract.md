# Bookland API — error contract

Every error response is `application/problem+json` ([RFC 7807](https://www.rfc-editor.org/rfc/rfc7807)),
in **English**, with one extension member on top of the standard ones:

| Member | Source | Notes |
|---|---|---|
| `type` | standard | omitted — it is `about:blank` for every error today, and Spring leaves the default out |
| `title` | standard | the HTTP reason phrase (`Unauthorized`, `Not Found`, …) |
| `status` | standard | mirrors the HTTP status line |
| `detail` | standard | human-readable, safe to show as a banner — **never parse it** |
| `instance` | standard | the request path |
| **`code`** | extension | stable machine-readable symbol — **branch on this** |

`detail` is prose and may be reworded at any time. `code` is part of the contract and only changes
with a breaking release.

## Authentication and authorization

The single most important rule: **401 means the credential is missing or no longer good; 403 means
the credential is fine but the role is not.** A 403 is never worth a token refresh.

| Situation | Status | `code` | `WWW-Authenticate` |
|---|---|---|---|
| No `Authorization` header | 401 | `TOKEN_MISSING` | `Bearer` |
| Access token expired | 401 | `TOKEN_EXPIRED` | `Bearer error="invalid_token", …` |
| Access token malformed / bad signature | 401 | `TOKEN_INVALID` | `Bearer error="invalid_token", …` |
| Authenticated, lacks the required role | 403 | `INSUFFICIENT_ROLE` | `Bearer error="insufficient_scope", …` |

Example:

```json
{
  "detail": "The access token has expired",
  "instance": "/api/v1/cart",
  "status": 401,
  "title": "Unauthorized",
  "code": "TOKEN_EXPIRED"
}
```

### How a client should react

| `code` | Reaction |
|---|---|
| `TOKEN_MISSING` | Send the user to login |
| `TOKEN_EXPIRED` | Refresh once, replay the original request; if the replay fails too, end the session |
| `TOKEN_INVALID` | End the session — refreshing will not help |
| `INSUFFICIENT_ROLE` | Show a forbidden screen; do **not** refresh, do **not** end the session |

Refreshing is no longer an API call: a client renews by exchanging its refresh token at the
Authorization Server's `POST /oauth2/token` with `grant_type=refresh_token`, and failures there are
OAuth2 errors (`{"error": "invalid_grant"}`), not this contract. `INVALID_CREDENTIALS` and
`INVALID_REFRESH_TOKEN` are gone with the endpoints that produced them — a wrong password is now
answered by the Authorization Server's login form, which is HTML, and never reaches the API.

**A rejected token now fails the request even on a public endpoint.** `GET /api/v1/books` with an
expired token answers 401 rather than serving the catalogue. This changed when the API became a
resource server: its filter refuses an unusable credential wherever one is presented, where the
previous hand-written filter recorded the reason and carried on. Routes that are not the API at all
— `/error`, `/media/**`, the console, the API document — are on a chain of their own and keep the
tolerant behaviour, which matters most for `/error`: the container forwards there carrying the
original headers, and answering that forward with 401 would hide every 500 behind an expired-session
message.

## Validation and malformed requests

A rejected payload carries an extra `errors` member — field name → the messages that field broke —
so each message can be rendered next to its own form input. `detail` stays populated as a summary
for a form-level banner.

| Situation | Status | `code` | `errors` |
|---|---|---|---|
| A field breaks a constraint | 400 | `VALIDATION_ERROR` | yes |
| A path variable or query parameter will not convert | 400 | `INVALID_PARAMETER` | yes |
| Body missing, truncated or not JSON | 400 | `MALFORMED_REQUEST` | no |

```json
{
  "detail": "Validation failed for 3 fields: email, name, password",
  "instance": "/api/v1/auth/register",
  "status": 400,
  "title": "Bad Request",
  "code": "VALIDATION_ERROR",
  "errors": {
    "email": ["must be a well-formed email address"],
    "name": ["must not be blank"],
    "password": ["must contain at least one number", "size must be between 8 and 72"]
  }
}
```

Rules a client can rely on:

- **A field can carry more than one message** — the values are always arrays, never strings.
- **Messages never name their own field** (`"must not be blank"`, not `"name must not be blank"`) —
  the map key already does, so a client can render `<field label> + <message>` itself.
- **Messages are always English**, whatever `Accept-Language` says and whatever locale the server
  runs in. `FixedLocaleMessageInterpolator` pins them, because Hibernate Validator otherwise
  resolves its built-in messages against the host JVM's default locale — which is how a single
  response used to mix Portuguese defaults with English custom messages.
- **Errors that belong to the payload as a whole**, not to one field, appear under the reserved key
  `"_"`.
- `errors` is absent, not empty, when the failure has no field to attach to.

Business rule violations are **not** validation errors: they carry `detail` and no `errors` map, and
are raised by each module's own `@RestControllerAdvice`.

## Server errors and unroutable requests

Anything no `@RestControllerAdvice` handles is forwarded by the container to `/error` and rendered
in the same shape by `ProblemDetailErrorController`:

| Situation | Status | `code` |
|---|---|---|
| Unhandled exception | 500 | `INTERNAL_ERROR` |
| No route matched (on a public path) | 404 | `NOT_FOUND` |
| Wrong method, unsupported media type, … | 4xx | the status name (`METHOD_NOT_ALLOWED`, …) |

A 5xx `detail` is always `"The server failed to process the request"` — the exception's own message
carries stack traces, SQL and column names. The cause is in the server log, never in the response.

⚠️ **`instance` is always the request that failed, never `/error`.** If you ever see
`"instance": "/error"`, the deployed build predates this and is masking a real failure.

**`/error` must stay `permitAll`.** Behind the authentication wall, the container's forward is
answered with `401 TOKEN_MISSING` and the real 500 never reaches the client — arriving as "your
session expired", which makes a client refresh, fail, and log the user out over a server bug.

Note that an unauthenticated request to an unmapped path *behind* the wall is still `401`, not
`404`: security runs before routing, and answering 404 there would let anyone map the private
routes.

## Business rule violations

| Module | `code` | Status |
|---|---|---|
| user | `USER_NOT_FOUND` | 404 |
| user | `EMAIL_ALREADY_EXISTS` | 409 |
| user | `USER_ACCESS_DENIED` | 403 |
| user | `ADMIN_ACCOUNT_NOT_DELETABLE` | 409 |
| catalog | `BOOK_NOT_FOUND` | 404 |
| catalog | `CATEGORY_NOT_FOUND` | 404 |
| catalog | `ISBN_ALREADY_EXISTS` | 409 |
| catalog | `BOOK_HAS_ACTIVE_ORDERS` | 409 |
| catalog | `INSUFFICIENT_STOCK` | 422 |
| catalog | `INVALID_IMAGE` | 422 |
| catalog | `FILE_TOO_LARGE` | 413 |
| catalog | `ORDERS_UNAVAILABLE` | 503 — orders could not say whether the book has active orders; the removal is refused, not done unchecked |
| orders | `CART_NOT_FOUND` | 404 — changing an item of a cart that does not exist |
| orders | `CART_EMPTY` | 409 — checkout with an empty or missing cart (it used to answer `CART_NOT_FOUND`, 404) |
| orders | `ORDER_NOT_FOUND` | 404 |
| orders | `BOOK_NOT_IN_CART` | 404 |
| orders | `ORDER_ACCESS_DENIED` | 403 |
| orders | `CART_ITEM_UNAVAILABLE` | 409 |
| orders | `ORDER_CANCELLATION_NOT_ALLOWED` | 409 — also while the checkout is still running (`PENDING`, `AWAITING_PAYMENT`) |
| orders | `INVALID_ORDER_STATUS_TRANSITION` | 409 |
| orders | `CHECKOUT_IN_PROGRESS` | 409 — a checkout for this customer has not finished yet |
| orders, wishlist, reviews | `CATALOG_UNAVAILABLE` | 503 — the catalog could not be asked about the book (down, past the deadline, or its circuit breaker open). Retry later; reading the cart or the wishlist does not fail, it shows the items as unavailable |
| payments | `PAYMENT_NOT_FOUND` | 404 |
| payments | `PAYMENT_ACCESS_DENIED` | 403 |
| payments | `REFUND_NOT_ALLOWED` | 409 |
| reviews | `REVIEW_NOT_FOUND` | 404 |
| reviews | `DUPLICATE_REVIEW` | 409 — also when two submissions race past the check: the database's unique index (one live review per customer and book) refuses the second |
| reviews | `REVIEW_ALREADY_DELETED` | 409 |
| reviews | `PURCHASE_REQUIRED` | 403 |
| wishlist | `WISHLIST_ITEM_NOT_FOUND` | 404 |
| wishlist | `WISHLIST_ITEM_ALREADY_EXISTS` | 409 |
| gateway | `UPSTREAM_TIMEOUT` | 504 — the service behind the gateway did not answer in time (connect 2 s, response 10 s) |
| gateway | `UPSTREAM_UNAVAILABLE` | 502 — the gateway could not talk to the service at all (connection refused or closed) |
| any | `INVALID_ARGUMENT` | 400 |

⚠️ **Not every 403 is a role problem.** `ORDER_ACCESS_DENIED` (someone else's order),
`USER_ACCESS_DENIED` (someone else's account), `PAYMENT_ACCESS_DENIED` (the payment of someone
else's order) and `PURCHASE_REQUIRED` (reviewing a book you have not bought) are 403s that say
nothing about the caller's role — only `INSUFFICIENT_ROLE` does. This is exactly why status alone
is not enough to branch on.

These four are **resource-ownership** rules and none of them can live in `ApiSecurityConfig`: whether
an order, an account or a payment is yours is not derivable from the URL and the token alone. They
belong to the use case that owns the aggregate, which takes the caller's id as a parameter — see
`GetOrderByIdService`, `GetUserProfileService`, `GetPaymentByOrderIdService`. A handler that
addresses a resource by id and does not declare an `AuthenticatedUser` parameter is the shape this
class of bug takes, and is worth treating as a review smell.

`BOOK_NOT_FOUND` is raised by the catalog, orders, reviews and wishlist advices alike; the code is the
same everywhere, so a client never has to care which module answered. Since step 5 the other modules
read books over gRPC, so each has its own exception for it — until then orders relied on the catalog's
advice, which was global in the monolith.

## In the OpenAPI document

`GET /api-docs` describes the contract, so a client can generate its error type instead of
hand-writing it:

- `components.schemas.ProblemDetail` — the members above, including `code`.
- `components.schemas.ValidationProblemDetail` — `allOf` ProblemDetail plus the `errors` map.
- Every operation carries a **`default`** response pointing at `ProblemDetail`, and every operation
  that takes a body or parameters also carries an explicit **`400`** pointing at
  `ValidationProblemDetail`.
- `components.securitySchemes.bearerAuth` — HTTP bearer, JWT.

Which of 401/403/404/409/422 a given endpoint can produce is **not** enumerated per operation. That
depends on `ApiSecurityConfig` rules and on domain exceptions that nothing on the handler declares, so
a hand-maintained list would go stale without anyone noticing; `default` is accurate and gives a
generator the one error type it needs.

Success codes are accurate too, but only because each handler says so: springdoc infers `200` from
the return type and cannot see through `ResponseEntity.status(...)`. **A handler answering anything
other than 200 must carry `@ResponseStatus`** — otherwise the document silently claims 200 and a
generated client gets the wrong success type. The eight that do are pinned in
`OpenApiErrorContractIntegrationTest`, and `WebLayerRulesTest` (ArchUnit, bookland-app) fails the
build on any handler that builds a non-200 `ResponseEntity` without declaring it — including new
ones.

## Implementation

- `AuthErrorCode` (bookland-web-support) holds the enum, the detail text and the request-attribute
  name used to carry the rejection reason out of the authenticating filter.
- `JwtAuthenticationFilter` (bookland-auth) records *why* a token was rejected; it never writes a
  response itself.
- `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` (bookland-web-support) turn that into
  the body above. They are wired in `ApiSecurityConfig.securityFilterChain`.
- `AuthErrorContractIntegrationTest` (bookland-app) locks the table above against the real filter
  chain.
- `ValidationExceptionHandler` (bookland-web-support) is the **single** advice handling bean
  validation for the whole application, at `HIGHEST_PRECEDENCE`. Modules must not add their own —
  two advices for the same exception leaves the winner up to bean ordering, and the payload shape
  drifts apart module by module. `ValidationErrorContractIntegrationTest` locks it.
- `ValidationConfig` (bookland-web-support) replaces Boot's auto-configured validator with the same
  one plus a fixed English locale.
- Each module's `*ExceptionHandler` builds its business errors through `ProblemDetails.of(status,
  detail, code)` — the code belongs next to the exception it describes, so the module owning the
  rule owns its symbol. `BusinessErrorContractIntegrationTest` covers the cases reachable without a
  fixture.
- `ErrorResponsesCustomizer` (bookland-web-support) puts the schemas and responses into the OpenAPI
  document; `OpenApiConfig` (bookland-web-support) registers it along with the API info and bearer scheme.
  `OpenApiErrorContractIntegrationTest` locks the published document.

Should the modules ever be split into separate services, this contract — not the code — is what has
to be preserved. A gateway or an OAuth2 resource server terminating the token in front of the
services must emit the same statuses, codes and bodies.

## Checkout outcomes are order statuses, not errors

Since the checkout became an asynchronous saga, `POST /api/v1/cart/checkout` answers **202** with the
order `PENDING` as soon as it starts. Whether it succeeded is not known at that moment, so it cannot be
an error response: the outcome is the order's `status`, read with `GET /api/v1/orders/{id}`.

| Outcome | `status` | `statusReason` |
|---|---|---|
| Stock reserved and payment approved | `CONFIRMED` | — |
| The stock ran out while the checkout ran | `REJECTED` | the books that were unavailable |
| The payment was declined (the reserved stock is given back) | `PAYMENT_FAILED` | the decline reason |

What still fails synchronously, before anything starts: an empty or missing cart (`CART_EMPTY`, 409),
stock that is visibly short already (`CART_ITEM_UNAVAILABLE`, 409) and a checkout already in progress
(`CHECKOUT_IN_PROGRESS`, 409). **`PAYMENT_DECLINED` (402) is retired**: a decline is now
`PAYMENT_FAILED` on the order.

A payment gateway that does not answer is **not** a decline: the order stays `AWAITING_PAYMENT` while
payments retries, for as long as the outage lasts, and then moves on as usual. A client polling the
order should keep polling, and read a long `AWAITING_PAYMENT` as "the payment is delayed", never as a
failure.

