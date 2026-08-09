package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.port.in.DeleteUserUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.exception.UserAccessDeniedException;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;

public class DeleteUserService implements DeleteUserUseCase {

    private final UserPersistencePort persistencePort;

    private DeleteUserService(UserPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public static DeleteUserService create(UserPersistencePort persistencePort) {
        return new DeleteUserService(persistencePort);
    }

    @Override
    public void execute(UserId id, UserId requesterId) {
        if (!id.equals(requesterId)) {
            throw new UserAccessDeniedException(id.value());
        }
        if (persistencePort.findById(id).isEmpty()) {
            throw new UserNotFoundException(id.value());
        }
        persistencePort.delete(id);
    }
}
