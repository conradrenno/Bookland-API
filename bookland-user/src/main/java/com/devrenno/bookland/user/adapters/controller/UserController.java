package com.devrenno.bookland.user.adapters.controller;

import com.devrenno.bookland.user.adapters.presenter.UserPresenter;
import com.devrenno.bookland.user.adapters.viewmodel.UserViewModel;
import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.port.in.DeleteUserUseCase;
import com.devrenno.bookland.user.application.port.in.GetUserByIdUseCase;
import com.devrenno.bookland.user.application.port.in.UpdateUserUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.application.service.DeleteUserService;
import com.devrenno.bookland.user.application.service.GetUserByIdService;
import com.devrenno.bookland.user.application.service.UpdateUserService;
import com.devrenno.bookland.user.domain.valueobject.UserId;

import java.util.UUID;

/**
 * Internal (Uncle Bob) controller: orchestrates the user's HTTP-facing use cases and delegates
 * to the Presenter. Also the composition root of the module's inner graph — it wires the use
 * cases from the outbound ports it receives (as interfaces) from infrastructure. Framework-free.
 */
public class UserController {

    private final GetUserByIdUseCase getUserByIdUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserPresenter presenter;

    private UserController(GetUserByIdUseCase getUserByIdUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UserPresenter presenter) {
        this.getUserByIdUseCase = getUserByIdUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.presenter = presenter;
    }

    public static UserController create(UserPersistencePort persistencePort) {
        return new UserController(
                GetUserByIdService.create(persistencePort),
                UpdateUserService.create(persistencePort),
                DeleteUserService.create(persistencePort),
                UserPresenter.create()
        );
    }

    public UserViewModel getById(UUID id) {
        return presenter.present(getUserByIdUseCase.execute(UserId.of(id)));
    }

    public UserViewModel update(UUID id, UpdateUserCommand command) {
        return presenter.present(updateUserUseCase.execute(UserId.of(id), command));
    }

    public void delete(UUID id) {
        deleteUserUseCase.execute(UserId.of(id));
    }
}
