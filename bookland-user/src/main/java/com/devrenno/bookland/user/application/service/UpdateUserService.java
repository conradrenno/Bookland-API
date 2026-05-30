package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.annotation.UseCase;
import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.mapper.UserApplicationMapper;
import com.devrenno.bookland.user.application.port.in.UpdateUserUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UpdateUserService implements UpdateUserUseCase {

    private final UserPersistencePort persistencePort;
    private final UserApplicationMapper mapper;

    @Override
    public UserResponse execute(UserId id, UpdateUserCommand command) {
        User user = persistencePort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.value()));
        if (command.name() != null) {
            user.updateName(command.name());
        }
        return mapper.toResponse(persistencePort.save(user));
    }
}
