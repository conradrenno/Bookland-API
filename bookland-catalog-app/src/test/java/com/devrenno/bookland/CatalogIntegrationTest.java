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
 * Every full-context test of the catalog service, the catalog's twin of the monolith's
 * {@code BooklandIntegrationTest}: MockMvc, the dev profile, an in-JVM Kafka broker (the catalog
 * consumes stock commands, OrderCancelled and BookRatingChanged, and produces stock replies), and the
 * test signing key in place of the identity service's JWKS.
 *
 * <p>gRPC runs in memory: the BookCatalog server answers on no port, and the {@code orders} channel —
 * like every channel — reaches the in-memory server, where {@link FakeOrderActivity} stands in for
 * the orders service the catalog asks before removing a book.
 *
 * <p>One annotation keeps the whole suite on a single cached context and a single broker.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
@EmbeddedKafka(bootstrapServersProperty = "spring.kafka.bootstrap-servers")
@TestPropertySource(properties = {"bookland.resource-server.jwk-set-uri=", "spring.grpc.test.inprocess.enabled=true"})
@Import({TestSigningKey.class, FakeOrderActivity.class})
public @interface CatalogIntegrationTest {
}
