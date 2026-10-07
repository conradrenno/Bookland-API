package com.devrenno.bookland;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The full application context, as every integration test boots it.
 *
 * <p>The context includes a Kafka producer and listener, so it gets an in-JVM broker: without one the
 * listener logs connection errors for the whole run and every review creation stalls on
 * {@code max.block.ms}. The embedded broker's address replaces {@code spring.kafka.bootstrap-servers}.
 *
 * <p>No identity service is running and MockMvc opens no port, so the token decoder must not fetch a
 * JWKS over HTTP: {@code jwk-set-uri} is blanked and {@link TestSigningKey} supplies the keys in
 * memory instead.
 *
 * <p>gRPC runs in process ({@code spring.grpc.test.inprocess.enabled}): the server listens on no port
 * and every channel, whatever address it is configured with, reaches it in memory — so the modules
 * still talk through their gRPC clients and servers, and a test run cannot collide with a running
 * application on 9090.
 *
 * <p>One annotation instead of four on each class is also what keeps the test-context cache to a
 * single context — and a single broker — for the whole suite: classes whose configuration differs
 * (one with MockMvc, one without) each boot their own.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestPropertySource(properties = {"bookland.resource-server.jwk-set-uri=", "spring.grpc.test.inprocess.enabled=true"})
@Import(TestSigningKey.class)
public @interface BooklandIntegrationTest {
}
