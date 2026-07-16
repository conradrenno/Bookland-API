package com.devrenno.bookland.user.application.service;

import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.port.in.RegisterUserUseCase;
import com.devrenno.bookland.user.application.port.out.PasswordEncoderPort;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.service.UserDomainService;
import com.devrenno.bookland.user.domain.valueobject.Email;

public class RegisterUserService implements RegisterUserUseCase {

    private final UserDomainService domainService;
    private final UserPersistencePort persistencePort;
    private final PasswordEncoderPort passwordEncoderPort;

    private RegisterUserService(UserDomainService domainService,
                                UserPersistencePort persistencePort,
                                PasswordEncoderPort passwordEncoderPort) {
        this.domainService = domainService;
        this.persistencePort = persistencePort;
        this.passwordEncoderPort = passwordEncoderPort;
    }

    public static RegisterUserService create(UserDomainService domainService,
                                             UserPersistencePort persistencePort,
                                             PasswordEncoderPort passwordEncoderPort) {
        return new RegisterUserService(domainService, persistencePort, passwordEncoderPort);
    }

    @Override
    public User execute(CreateUserCommand command) {
        Email email = Email.of(command.email());
        boolean emailExists = persistencePort.existsByEmail(email);

        User user = User.create(
                command.name(),
                email,
                passwordEncoderPort.encode(command.rawPassword()),
                command.role()
        );
        domainService.validateForCreation(user, emailExists);

        return persistencePort.save(user);
    }
}
