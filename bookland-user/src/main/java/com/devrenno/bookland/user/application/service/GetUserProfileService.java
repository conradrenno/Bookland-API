package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.port.in.GetUserProfileUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.UserAccessDeniedException;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;

public class GetUserProfileService implements GetUserProfileUseCase {

    private final UserPersistencePort persistencePort;

    private GetUserProfileService(UserPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public static GetUserProfileService create(UserPersistencePort persistencePort) {
        return new GetUserProfileService(persistencePort);
    }

    /**
     * Ownership is checked before the lookup, not after. For this aggregate the owner is the
     * identifier itself, so no read is needed to decide — which means a caller probing other
     * people's ids gets the same 403 whether or not the account exists, and the route is not an
     * oracle for which user ids are real.
     */
    @Override
    public User execute(UserId id, UserId requesterId) {
        if (!id.equals(requesterId)) {
            throw new UserAccessDeniedException(id.value());
        }
        return persistencePort.findById(id)
                .filter(User::isActive)
                .orElseThrow(() -> new UserNotFoundException(id.value()));
    }
}
