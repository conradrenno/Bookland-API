package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.annotation.UseCase;
import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.mapper.UserApplicationMapper;
import com.devrenno.bookland.user.application.port.in.RegisterUserUseCase;
import com.devrenno.bookland.user.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.service.UserDomainService;
import com.devrenno.bookland.user.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class RegisterUserService implements RegisterUserUseCase {

    private final UserDomainService domainService;
    private final UserPersistencePort persistencePort;
    private final PasswordEncoderPort passwordEncoderPort;
    private final UserApplicationMapper mapper;

    @Override
    public UserResponse execute(CreateUserCommand command) {
        Email email = Email.of(command.email());
        boolean emailExists = persistencePort.existsByEmail(email);

        User user = User.create(
                command.name(),
                email,
                passwordEncoderPort.encode(command.rawPassword()),
                command.role()
        );
        domainService.validateForCreation(user, emailExists);

        return mapper.toResponse(persistencePort.save(user));
    }
}
