package com.devrenno.bookland.auth.api.mapper;

import com.devrenno.bookland.auth.api.dto.LoginRequest;
import com.devrenno.bookland.auth.api.dto.TokenApiResponse;
import com.devrenno.bookland.auth.application.dto.LoginCommand;
import com.devrenno.bookland.auth.application.dto.TokenResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface AuthApiMapper {

    @Mapping(source = "password", target = "rawPassword")
    LoginCommand toCommand(LoginRequest request);

    TokenApiResponse toApiResponse(TokenResponse response);
}
