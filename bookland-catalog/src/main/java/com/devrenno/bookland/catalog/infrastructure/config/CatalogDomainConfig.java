package com.devrenno.bookland.catalog.infrastructure.config;

import com.devrenno.bookland.catalog.domain.service.CatalogDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CatalogDomainConfig {

    @Bean
    public CatalogDomainService catalogDomainService() {
        return new CatalogDomainService();
    }
}
