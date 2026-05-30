package com.devrenno.bookland.user.api;

import com.devrenno.bookland.user.api.controller.UserController;
import com.devrenno.bookland.user.api.dto.request.CreateUserRequest;
import com.devrenno.bookland.user.api.dto.response.UserApiResponse;
import com.devrenno.bookland.user.api.mapper.UserApiMapper;
import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.application.port.in.*;
import com.devrenno.bookland.user.domain.entity.UserRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private RegisterUserUseCase registerUserUseCase;
    @Mock private GetUserByIdUseCase getUserByIdUseCase;
    @Mock private UpdateUserUseCase updateUserUseCase;
    @Mock private DeleteUserUseCase deleteUserUseCase;
    @Mock private UserApiMapper mapper;
    @InjectMocks private UserController controller;

    @Test
    void register_shouldReturn201_whenRequestIsValid() {
        UUID id = UUID.randomUUID();
        CreateUserRequest request = new CreateUserRequest("Alice", "alice@test.com", "secret123", UserRole.CUSTOMER);
        CreateUserCommand command = new CreateUserCommand("Alice", "alice@test.com", "secret123", UserRole.CUSTOMER);
        UserResponse appResponse = new UserResponse(id, "Alice", "alice@test.com",
                UserRole.CUSTOMER, LocalDateTime.now(), LocalDateTime.now(), true, "hashed");
        UserApiResponse apiResponse = new UserApiResponse(id, "Alice", "alice@test.com",
                "CUSTOMER", LocalDateTime.now(), LocalDateTime.now(), true);

        when(mapper.toCommand(request)).thenReturn(command);
        when(registerUserUseCase.execute(command)).thenReturn(appResponse);
        when(mapper.toApiResponse(appResponse)).thenReturn(apiResponse);

        ResponseEntity<UserApiResponse> result = controller.register(request);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().email()).isEqualTo("alice@test.com");
    }

    @Test
    void delete_shouldReturn204_whenUserExists() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.delete(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}
