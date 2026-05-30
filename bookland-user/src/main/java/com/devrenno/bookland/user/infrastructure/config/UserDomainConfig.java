package com.devrenno.bookland.user.infrastructure.config;

import com.devrenno.bookland.user.domain.service.UserDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class UserDomainConfig {

    @Bean
    public UserDomainService userDomainService() {
        return new UserDomainService();
    }
}
