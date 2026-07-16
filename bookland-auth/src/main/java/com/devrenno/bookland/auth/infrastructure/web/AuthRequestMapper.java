package com.devrenno.bookland.auth.infrastructure.web;

import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.infrastructure.web.dto.LoginRequest;
import com.devrenno.bookland.auth.infrastructure.web.dto.RegisterRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthRequestMapper {

    @Mapping(source = "password", target = "rawPassword")
    LoginCommand toCommand(LoginRequest request);

    @Mapping(source = "password", target = "rawPassword")
    RegisterCommand toRegisterCommand(RegisterRequest request);
}
