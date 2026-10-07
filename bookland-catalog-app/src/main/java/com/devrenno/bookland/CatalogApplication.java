package com.devrenno.bookland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The catalog service: catalog + inventory, their own database, HTTP for clients and gRPC for the
 * other services. Lives in the root package {@code com.devrenno.bookland} on purpose: component,
 * entity and repository scanning start here and reach every module this application's pom puts on
 * the classpath — moving it into a subpackage would silently drop them all.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class CatalogApplication {

    public static void main(String[] args) {
        SpringApplication.run(CatalogApplication.class, args);
    }
}
