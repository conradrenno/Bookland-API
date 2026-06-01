package com.devrenno.bookland.auth.infrastructure.adapter;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.port.out.UserRegistrationPort;
import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.port.in.RegisterUserUseCase;
import com.devrenno.bookland.user.domain.entity.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserRegistrationAdapter implements UserRegistrationPort {

    private final RegisterUserUseCase registerUserUseCase;

    @Override
    public AuthUserDto register(String name, String email, String rawPassword) {
        var response = registerUserUseCase.execute(
                new CreateUserCommand(name, email, rawPassword, UserRole.CUSTOMER)
        );
        return new AuthUserDto(response.id(), response.email(), response.passwordHash(), response.role().name());
    }
}
