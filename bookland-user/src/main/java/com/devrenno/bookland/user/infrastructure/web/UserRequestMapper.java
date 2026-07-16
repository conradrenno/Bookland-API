package com.devrenno.bookland.user.infrastructure.web;

import com.devrenno.bookland.user.application.dto.UpdateUserCommand;
import com.devrenno.bookland.user.infrastructure.web.dto.UpdateUserRequest;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UserRequestMapper {

    UpdateUserCommand toCommand(UpdateUserRequest request);
}
