package com.devrenno.bookland.user.infrastructure.mapper;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import com.devrenno.bookland.user.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserPersistenceMapper {

    default User toDomain(UserJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return User.reconstitute(
                UserId.of(entity.getId()),
                entity.getName(),
                Email.of(entity.getEmail()),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.isActive()
        );
    }

    @Mapping(target = "id", expression = "java(user.getId().value())")
    @Mapping(target = "email", expression = "java(user.getEmail().value())")
    UserJpaEntity toEntity(User user);
}
