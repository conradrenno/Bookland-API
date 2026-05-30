package com.devrenno.bookland.user.application.mapper;

import com.devrenno.bookland.user.application.dto.UserResponse;
import com.devrenno.bookland.user.domain.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserApplicationMapper {

    @Mapping(target = "id", expression = "java(user.getId().value())")
    @Mapping(target = "email", expression = "java(user.getEmail().value())")
    UserResponse toResponse(User user);
}