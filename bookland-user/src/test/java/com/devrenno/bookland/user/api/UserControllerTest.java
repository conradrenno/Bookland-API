package com.devrenno.bookland.user.api;

import com.devrenno.bookland.user.api.controller.UserController;
import com.devrenno.bookland.user.api.dto.response.UserApiResponse;
import com.devrenno.bookland.user.api.mapper.UserApiMapper;
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

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock private GetUserByIdUseCase getUserByIdUseCase;
    @Mock private UpdateUserUseCase updateUserUseCase;
    @Mock private DeleteUserUseCase deleteUserUseCase;
    @Mock private UserApiMapper mapper;
    @InjectMocks private UserController controller;

    @Test
    void delete_shouldReturn204_whenUserExists() {
        UUID id = UUID.randomUUID();

        ResponseEntity<Void> result = controller.delete(id);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }
}