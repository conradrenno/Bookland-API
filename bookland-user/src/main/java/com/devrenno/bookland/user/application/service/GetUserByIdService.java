package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.port.in.GetUserByIdUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;

public class GetUserByIdService implements GetUserByIdUseCase {

    private final UserPersistencePort persistencePort;

    private GetUserByIdService(UserPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public static GetUserByIdService create(UserPersistencePort persistencePort) {
        return new GetUserByIdService(persistencePort);
    }

    @Override
    public User execute(UserId id) {
        return persistencePort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.value()));
    }
}
