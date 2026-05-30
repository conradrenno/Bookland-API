package com.devrenno.bookland.user.api.mapper;

import com.devrenno.bookland.user.api.dto.request.CreateUserRequest;
import com.devrenno.bookland.user.api.dto.request.UpdateUserRequest;
import com.devrenno.bookland.user.api.dto.response.UserApiResponse;
import com.devrenno.bookland.user.application.dto.CreateUserCommand;
import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.application.dto.UserResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserApiMapper {

    @Mapping(source = "password", target = "rawPassword")
    CreateUserCommand toCommand(CreateUserRequest request);

    UpdateUserCommand toCommand(UpdateUserRequest request);

    @Mapping(target = "role", expression = "java(response.role().name())")
    UserApiResponse toApiResponse(UserResponse response);
}
