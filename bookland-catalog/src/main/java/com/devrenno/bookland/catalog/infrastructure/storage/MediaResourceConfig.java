package com.devrenno.bookland.catalog.infrastructure.storage;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/**
 * Serves stored cover images as static resources under {@code /media/covers/**},
 * mapping the public path used by {@link LocalImageStorageAdapter} to the storage directory.
 */
@Configuration
public class MediaResourceConfig implements WebMvcConfigurer {

    private final String location;

    public MediaResourceConfig(@Value("${bookland.storage.covers-location}") String location) {
        this.location = location;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String fileUri = Path.of(location).toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler(LocalImageStorageAdapter.PUBLIC_PATH + "/**")
                .addResourceLocations(fileUri);
    }
}
