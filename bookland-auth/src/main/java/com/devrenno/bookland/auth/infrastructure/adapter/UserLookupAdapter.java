package com.devrenno.bookland.auth.infrastructure.adapter;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.port.in.GetUserByEmailUseCase;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.Email;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookupPort {

    private final GetUserByEmailUseCase getUserByEmailUseCase;

    @Override
    public Optional<AuthUserDto> findByEmail(String email) {
        try {
            UserResponse response = getUserByEmailUseCase.execute(Email.of(email));
            return Optional.of(new AuthUserDto(
                    response.id(),
                    response.email(),
                    response.passwordHash(),
                    response.role()
            ));
        } catch (UserNotFoundException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
