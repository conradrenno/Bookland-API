package com.devrenno.bookland;

import com.devrenno.bookland.notification.application.port.out.EmailTaskQueuePort;
import com.devrenno.bookland.notification.infrastructure.messaging.RabbitEmailTaskQueueAdapter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The notification service boots with its module wired and its own schema migrated — the real
 * adapters included, even though the tests put doubles in front of them.
 */
@NotificationIntegrationTest
class NotificationApplicationTests {

    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private ApplicationContext context;

    @Test
    @DisplayName("the context loads; Flyway created the inbox")
    void contextLoads() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from notification_inbox", Integer.class)).isNotNull();
    }

    @Test
    @DisplayName("the RabbitMQ adapter is wired, behind the test's in-memory queue")
    void realQueueAdapterIsWired() {
        assertThat(context.getBeansOfType(EmailTaskQueuePort.class).values())
                .anySatisfy(port -> assertThat(port).isInstanceOf(RabbitEmailTaskQueueAdapter.class));
    }

    /** No web server: the service has no HTTP API, so nothing listens on a port but its clients. */
    @Test
    @DisplayName("no web server in the context")
    void noWebServer() {
        assertThat(context.getClass().getName()).doesNotContain("Web");
    }
}
