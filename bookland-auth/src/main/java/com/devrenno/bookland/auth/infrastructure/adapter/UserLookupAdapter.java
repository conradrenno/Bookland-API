package com.devrenno.bookland.auth.infrastructure.adapter;

import com.devrenno.bookland.auth.application.dto.AuthUserDto;
import com.devrenno.bookland.auth.application.port.out.UserLookupPort;
import com.devrenno.bookland.user.application.port.in.GetUserByEmailUseCase;
import com.devrenno.bookland.user.application.port.in.GetUserByIdUseCase;
import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.UserNotFoundException;
import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class UserLookupAdapter implements UserLookupPort {

    private final GetUserByEmailUseCase getUserByEmailUseCase;
    private final GetUserByIdUseCase getUserByIdUseCase;

    @Override
    public Optional<AuthUserDto> findByEmail(String email) {
        try {
            return Optional.of(toDto(getUserByEmailUseCase.execute(Email.of(email))));
        } catch (UserNotFoundException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<AuthUserDto> findActiveById(UUID id) {
        try {
            return Optional.of(toDto(getUserByIdUseCase.execute(UserId.of(id))));
        } catch (UserNotFoundException e) {
            return Optional.empty();
        }
    }

    private static AuthUserDto toDto(User user) {
        return new AuthUserDto(
                user.getId().value(),
                user.getEmail().value(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive()
        );
    }
}
