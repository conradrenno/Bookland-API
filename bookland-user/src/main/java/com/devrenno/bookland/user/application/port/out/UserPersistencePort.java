package com.devrenno.bookland.user.application.port.out;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;

import java.util.Optional;

public interface UserPersistencePort {
    User save(User user);
    Optional<User> findById(UserId id);
    Optional<User> findByEmail(Email email);
    void delete(UserId id);
    boolean existsByEmail(Email email);
}
