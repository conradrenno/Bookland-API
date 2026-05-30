package com.devrenno.bookland.user.application.controller;

import com.devrenno.bookland.user.application.annotation.UseCase;
import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.port.in.*;
import com.devrenno.bookland.user.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.service.UserDomainService;
import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;

@UseCase
@RequiredArgsConstructor
public class UserApplicationController
        implements RegisterUserUseCase, GetUserByIdUseCase, GetUserByEmailUseCase,
                   UpdateUserUseCase, DeleteUserUseCase {

    private final UserDomainService domainService;
    private final UserPersistencePort persistencePort;
    private final PasswordEncoderPort passwordEncoderPort;

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

        User saved = persistencePort.save(user);
        return toResponse(saved);
    }

    @Override
    public UserResponse execute(UserId id) {
        User user = persistencePort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.value()));
        return toResponse(user);
    }

    @Override
    public UserResponse execute(Email email) {
        User user = persistencePort.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User not found with email: " + email.value()));
        return toResponse(user);
    }

    @Override
    public UserResponse execute(UserId id, UpdateUserCommand command) {
        User user = persistencePort.findById(id)
                .orElseThrow(() -> new UserNotFoundException(id.value()));
        if (command.name() != null) {
            user.updateName(command.name());
        }
        User updated = persistencePort.save(user);
        return toResponse(updated);
    }

    @Override
    public void delete(UserId id) {
        if (persistencePort.findById(id).isEmpty()) {
            throw new UserNotFoundException(id.value());
        }
        persistencePort.delete(id);
    }

    private UserResponse toResponse(User user) {
        return new UserResponse(
                user.getId().value(),
                user.getName(),
                user.getEmail().value(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.isActive(),
                user.getPasswordHash()
        );
    }
}
