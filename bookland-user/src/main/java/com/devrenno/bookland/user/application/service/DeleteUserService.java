package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.annotation.UseCase;
import com.devrenno.bookland.user.application.port.in.DeleteUserUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class DeleteUserService implements DeleteUserUseCase {

    private final UserPersistencePort persistencePort;

    @Override
    public void execute(UserId id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new UserNotFoundException(id.value());
        }
        persistencePort.delete(id);
    }
}
