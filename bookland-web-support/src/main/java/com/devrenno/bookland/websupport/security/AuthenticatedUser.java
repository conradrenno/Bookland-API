package com.devrenno.bookland.websupport.security;

import java.util.UUID;

/**
 * The caller behind the current request, as the authenticating filter established it.
 *
 * <p>A handler takes one by declaring it as a parameter; {@link AuthenticatedUserArgumentResolver}
 * supplies it. That resolver is the <em>only</em> place that knows how identity is carried on the
 * {@code Authentication} — which is exactly the knowledge that changes when the authentication
 * mechanism does, and which used to be copy-pasted into six controllers as an {@code instanceof}
 * over {@code getDetails()}.
 *
 * <p>Deliberately carries no role. Authorization by role belongs to {@code SecurityConfig}, in one
 * place; handing controllers a role invites a second, divergent copy of those rules. What a handler
 * legitimately needs is <em>who</em> is calling, to pass down to use cases whose rules are about
 * ownership rather than authority ({@code ORDER_ACCESS_DENIED}, {@code PURCHASE_REQUIRED}).
 *
 * @param id    the Bookland user id — the value domain modules store as {@code customer_id}
 * @param email the caller's email, as carried by the credential
 */
public record AuthenticatedUser(UUID id, String email) {
}
