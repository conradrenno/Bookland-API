package com.devrenno.bookland.user.adapters.presenter;

import com.devrenno.bookland.user.adapters.viewmodel.UserViewModel;
import com.devrenno.bookland.user.domain.entity.User;

/**
 * Transforms a domain User into the delivery-facing UserViewModel. Plain Java (no framework).
 */
public class UserPresenter {

    private UserPresenter() {
    }

    public static UserPresenter create() {
        return new UserPresenter();
    }

    public UserViewModel present(User user) {
        return new UserViewModel(
                user.getId().value(),
                user.getName(),
                user.getEmail().value(),
                user.getRole(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.isActive()
        );
    }
}
