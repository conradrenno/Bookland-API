package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.annotation.UseCase;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.mapper.UserApplicationMapper;
import com.devrenno.bookland.user.application.port.in.GetUserByEmailUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class GetUserByEmailService implements GetUserByEmailUseCase {

    private final UserPersistencePort persistencePort;
    private final UserApplicationMapper mapper;

    @Override
    public UserResponse execute(Email email) {
        return persistencePort.findByEmail(email)
                .map(mapper::toResponse)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email.value()));
    }
}
