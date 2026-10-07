package com.devrenno.bookland.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * The single entry point clients call: it routes each request to the service that owns it and does
 * nothing else — no domain module, no database, no token validation (every service stays a resource
 * server, a decision of step 1). The routes are configuration, in application.yml.
 *
 * <p>In a package of its own rather than the root {@code com.devrenno.bookland} the applications use:
 * there are no modules here to scan, and nothing should be picked up by accident.
 */
@SpringBootApplication
public class GatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
