package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.annotation.UseCase;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.mapper.UserApplicationMapper;
import com.devrenno.bookland.user.application.port.in.GetUserByIdUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserByIdService implements GetUserByIdUseCase {

    private final UserPersistencePort persistencePort;
    private final UserApplicationMapper mapper;

    @Override
    public UserResponse execute(UserId id) {
        return persistencePort.findById(id)
                .map(mapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException(id.value()));
    }
}
