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
| `POST /auth/login` with bad credentials | 401 | `INVALID_CREDENTIALS` | — |
| `POST /auth/refresh` or `/auth/logout` with a dead refresh token | 401 | `INVALID_REFRESH_TOKEN` | — |

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
| `INVALID_CREDENTIALS` | Show a login error |
| `INVALID_REFRESH_TOKEN` | End the session and send the user to login |

Note that a stale token on a **public** endpoint (`GET /api/v1/books/**`, …) does not fail the
request: the filter chain carries on unauthenticated and the endpoint answers normally.

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

## Business rule violations

| Module | `code` | Status |
|---|---|---|
| user | `USER_NOT_FOUND` | 404 |
| user | `EMAIL_ALREADY_EXISTS` | 409 |
| catalog | `BOOK_NOT_FOUND` | 404 |
| catalog | `CATEGORY_NOT_FOUND` | 404 |
| catalog | `ISBN_ALREADY_EXISTS` | 409 |
| catalog | `BOOK_HAS_ACTIVE_ORDERS` | 409 |
| catalog | `INSUFFICIENT_STOCK` | 422 |
| catalog | `INVALID_IMAGE` | 422 |
| catalog | `FILE_TOO_LARGE` | 413 |
| orders | `CART_NOT_FOUND` | 404 |
| orders | `ORDER_NOT_FOUND` | 404 |
| orders | `BOOK_NOT_IN_CART` | 404 |
| orders | `ORDER_ACCESS_DENIED` | 403 |
| orders | `CART_ITEM_UNAVAILABLE` | 409 |
| orders | `ORDER_CANCELLATION_NOT_ALLOWED` | 409 |
| orders | `INVALID_ORDER_STATUS_TRANSITION` | 409 |
| orders | `PAYMENT_DECLINED` | 402 |
| payments | `PAYMENT_NOT_FOUND` | 404 |
| payments | `REFUND_NOT_ALLOWED` | 409 |
| reviews | `REVIEW_NOT_FOUND` | 404 |
| reviews | `DUPLICATE_REVIEW` | 409 |
| reviews | `REVIEW_ALREADY_DELETED` | 409 |
| reviews | `PURCHASE_REQUIRED` | 403 |
| wishlist | `WISHLIST_ITEM_NOT_FOUND` | 404 |
| wishlist | `WISHLIST_ITEM_ALREADY_EXISTS` | 409 |
| any | `INVALID_ARGUMENT` | 400 |

⚠️ **Not every 403 is a role problem.** `ORDER_ACCESS_DENIED` (someone else's order) and
`PURCHASE_REQUIRED` (reviewing a book you have not bought) are 403s that say nothing about the
caller's role — only `INSUFFICIENT_ROLE` does. This is exactly why status alone is not enough to
branch on.

`BOOK_NOT_FOUND` is raised by the catalog, reviews and wishlist advices alike; the code is the same
everywhere, so a client never has to care which module answered.

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
depends on `SecurityConfig` rules and on domain exceptions that nothing on the handler declares, so
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
  the body above. They are wired in `SecurityConfig.securityFilterChain`.
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
  document; `OpenApiConfig` (bookland-app) registers it along with the API info and bearer scheme.
  `OpenApiErrorContractIntegrationTest` locks the published document.

Should the modules ever be split into separate services, this contract — not the code — is what has
to be preserved. A gateway or an OAuth2 resource server terminating the token in front of the
services must emit the same statuses, codes and bodies.
