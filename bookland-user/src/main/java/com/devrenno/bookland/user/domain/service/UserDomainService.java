package com.devrenno.bookland.user.domain.service;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.exception.EmailAlreadyExistsException;

public class UserDomainService {

    public void validateForCreation(User user, boolean emailAlreadyExists) {
        if (emailAlreadyExists) {
            throw new EmailAlreadyExistsException(user.getEmail().value());
        }
    }
}
