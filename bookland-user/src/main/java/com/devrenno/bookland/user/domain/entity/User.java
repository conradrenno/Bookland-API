package com.devrenno.bookland.user.domain.entity;

import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class User {

    private final UserId id;
    private String name;
    private final Email email;
    private String passwordHash;
    private UserRole role;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean active;

    public static User create(String name, Email email, String passwordHash, UserRole role) {
        return User.builder()
                .id(UserId.generate())
                .name(name)
                .email(email)
                .passwordHash(passwordHash)
                .role(role != null ? role : UserRole.CUSTOMER)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .active(true)
                .build();
    }

    public void updateName(String name) {
        this.name = name;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
