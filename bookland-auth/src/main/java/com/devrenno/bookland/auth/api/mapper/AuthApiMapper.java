package com.devrenno.bookland.auth.api.mapper;

import com.devrenno.bookland.auth.api.dto.LoginRequest;
import com.devrenno.bookland.auth.api.dto.RegisterRequest;
import com.devrenno.bookland.auth.api.dto.TokenApiResponse;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.dto.RegisterCommand;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthApiMapper {

    @Mapping(source = "password", target = "rawPassword")
    LoginCommand toCommand(LoginRequest request);

    @Mapping(source = "password", target = "rawPassword")
    RegisterCommand toRegisterCommand(RegisterRequest request);

    TokenApiResponse toApiResponse(TokenResponse response);
}
