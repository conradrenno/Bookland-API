package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.port.in.GetUserByEmailUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.Email;

public class GetUserByEmailService implements GetUserByEmailUseCase {

    private final UserPersistencePort persistencePort;

    private GetUserByEmailService(UserPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public static GetUserByEmailService create(UserPersistencePort persistencePort) {
        return new GetUserByEmailService(persistencePort);
    }

    @Override
    public User execute(Email email) {
        return persistencePort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email.value()));
    }
}
