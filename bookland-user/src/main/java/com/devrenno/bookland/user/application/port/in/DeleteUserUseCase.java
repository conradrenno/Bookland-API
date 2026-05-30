package com.devrenno.bookland.user.application.port.in;

import com.devrenno.bookland.user.domain.valueobject.UserId;

public interface DeleteUserUseCase {
    void delete(UserId id);
}
