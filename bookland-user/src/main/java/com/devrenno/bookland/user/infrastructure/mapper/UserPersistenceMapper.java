package com.devrenno.bookland.user.infrastructure.mapper;

import com.devrenno.bookland.user.domain.entity.User;
import com.devrenno.bookland.user.domain.valueobject.Email;
import com.devrenno.bookland.user.domain.valueobject.UserId;
import com.devrenno.bookland.user.infrastructure.persistence.entity.UserJpaEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {UserId.class, Email.class})
public interface UserPersistenceMapper {

    @Mapping(target = "id", expression = "java(UserId.of(entity.getId()))")
    @Mapping(target = "email", expression = "java(Email.of(entity.getEmail()))")
    User toDomain(UserJpaEntity entity);

    @Mapping(target = "id", expression = "java(user.getId().value())")
    @Mapping(target = "email", expression = "java(user.getEmail().value())")
    UserJpaEntity toEntity(User user);
}
