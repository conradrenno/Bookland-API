package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.UserId;

/**
 * Reads an account on behalf of a caller, who may only read their own.
 *
 * <p>Separate from {@link GetUserByIdUseCase} on purpose, and the separation is the point. That one
 * is a <em>system</em> lookup — reviews resolves an author's display name through it, legitimately
 * reading accounts that are not the caller's. This one is a <em>user-facing</em> read, where
 * reading someone else's account is the whole of the vulnerability.
 *
 * <p>Both served the same interface once, so the HTTP route inherited the system lookup's absence
 * of an owner check and any authenticated user could read any account. One use case cannot carry
 * two authorization rules; the fix is two use cases.
 */
public interface GetUserProfileUseCase {

    User execute(UserId id, UserId requesterId);
}
