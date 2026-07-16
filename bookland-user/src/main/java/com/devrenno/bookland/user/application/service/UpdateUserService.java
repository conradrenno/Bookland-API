package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.port.in.UpdateUserUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;

public class UpdateUserService implements UpdateUserUseCase {

    private final UserPersistencePort persistencePort;

    private UpdateUserService(UserPersistencePort persistencePort) {
        this.persistencePort = persistencePort;
    }

    public static UpdateUserService create(UserPersistencePort persistencePort) {
        return new UpdateUserService(persistencePort);
    }

    @Override
    public User execute(UserId id, UpdateUserCommand command) {
        User user = persistencePort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.value()));
        if (command.name() != null) {
            user.updateName(command.name());
        }
        return persistencePort.save(user);
    }
}
