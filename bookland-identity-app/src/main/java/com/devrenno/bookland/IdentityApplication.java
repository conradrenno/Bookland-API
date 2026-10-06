package com.devrenno.bookland;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * The identity service: the user module and the Authorization Server, in a process of their own.
 *
 * <p>In the root package on purpose. Component scanning, entity scanning and the JPA repositories
 * all start from this class's package, and the modules it assembles live under
 * {@code com.devrenno.bookland.user}, {@code .auth} and {@code .websupport}.
 *
 * <p>{@link ConfigurationPropertiesScan} is what registers {@code AdminProperties}; without it
 * {@code AdminBootstrap} has nothing to inject and the context does not start.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class IdentityApplication {

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }
}
