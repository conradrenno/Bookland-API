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

## Implementation

- `AuthErrorCode` (bookland-web-support) holds the enum, the detail text and the request-attribute
  name used to carry the rejection reason out of the authenticating filter.
- `JwtAuthenticationFilter` (bookland-auth) records *why* a token was rejected; it never writes a
  response itself.
- `RestAuthenticationEntryPoint` / `RestAccessDeniedHandler` (bookland-web-support) turn that into
  the body above. They are wired in `SecurityConfig.securityFilterChain`.
- `AuthErrorContractIntegrationTest` (bookland-app) locks the table above against the real filter
  chain.

Should the modules ever be split into separate services, this contract — not the code — is what has
to be preserved. A gateway or an OAuth2 resource server terminating the token in front of the
services must emit the same statuses, codes and bodies.
