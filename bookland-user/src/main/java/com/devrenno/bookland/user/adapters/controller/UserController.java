package com.devrenno.bookland.user.adapters.controller;

import com.devrenno.bookland.user.adapters.presenter.UserPresenter;
import com.devrenno.bookland.user.adapters.viewmodel.UserViewModel;
import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.port.in.DeleteUserUseCase;
import com.devrenno.bookland.user.application.port.in.GetUserProfileUseCase;
import com.devrenno.bookland.user.application.port.in.UpdateUserUseCase;
import com.devrenno.bookland.user.application.port.out.UserPersistencePort;
import com.devrenno.bookland.user.application.service.DeleteUserService;
import com.devrenno.bookland.user.application.service.GetUserProfileService;
import com.devrenno.bookland.user.application.service.UpdateUserService;
import com.devrenno.bookland.user.domain.valueobject.UserId;

import java.util.UUID;

/**
 * Internal (Uncle Bob) controller: orchestrates the user's HTTP-facing use cases and delegates
 * to the Presenter. Also the composition root of the module's inner graph — it wires the use
 * cases from the outbound ports it receives (as interfaces) from infrastructure. Framework-free.
 *
 * <p>Every operation here takes the caller's id alongside the target id and hands both to the use
 * case, which is where the owner check lives. The controller does not check anything itself: a rule
 * enforced here would protect this HTTP route and nothing else, and the point of putting it in the
 * use case is that it holds for every future consumer of the same operation.
 */
public class UserController {

    private final GetUserProfileUseCase getUserProfileUseCase;
    private final UpdateUserUseCase updateUserUseCase;
    private final DeleteUserUseCase deleteUserUseCase;
    private final UserPresenter presenter;

    private UserController(GetUserProfileUseCase getUserProfileUseCase,
                          UpdateUserUseCase updateUserUseCase,
                          DeleteUserUseCase deleteUserUseCase,
                          UserPresenter presenter) {
        this.getUserProfileUseCase = getUserProfileUseCase;
        this.updateUserUseCase = updateUserUseCase;
        this.deleteUserUseCase = deleteUserUseCase;
        this.presenter = presenter;
    }

    public static UserController create(UserPersistencePort persistencePort) {
        return new UserController(
                GetUserProfileService.create(persistencePort),
                UpdateUserService.create(persistencePort),
                DeleteUserService.create(persistencePort),
                UserPresenter.create()
        );
    }

    public UserViewModel getById(UUID id, UUID requesterId) {
        return presenter.present(getUserProfileUseCase.execute(UserId.of(id), UserId.of(requesterId)));
    }

    public UserViewModel update(UUID id, UUID requesterId, UpdateUserCommand command) {
        return presenter.present(updateUserUseCase.execute(UserId.of(id), UserId.of(requesterId), command));
    }

    public void delete(UUID id, UUID requesterId) {
        deleteUserUseCase.execute(UserId.of(id), UserId.of(requesterId));
    }
}
