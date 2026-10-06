package com.devrenno.bookland;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The identity service's full context, as every integration test here boots it.
 *
 * <p>The monolith's {@code @BooklandIntegrationTest} without the embedded Kafka broker: this service
 * neither publishes nor consumes events. One annotation for the whole suite keeps it on a single
 * cached context.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
public @interface IdentityIntegrationTest {
}
