package com.devrenno.bookland.notification.infrastructure.messaging;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The tests of the whole service replace the queue with a double (RabbitMQ has no embedded broker),
 * so the bytes that travel through the real one are checked here: what the publisher writes, the
 * listener reads back unchanged.
 */
class EmailTaskCodecTest {

    private final EmailTaskCodec codec = new EmailTaskCodec(JsonMapper.builder().build());

    @Test
    void aTaskSurvivesTheTripThroughTheQueue() {
        EmailTask task = new EmailTask("order-1:CONFIRMED", "reader@bookland.com", "Your order — confirmed",
                "Hello Ana,\n\n2 x Clean Code — 29.90\nTotal: 59.80");

        Message message = codec.toMessage(task);

        assertThat(codec.fromMessage(message)).isEqualTo(task);
    }

    @Test
    void theMessageIsPersistentJsonWithNoTypeHeader() {
        Message message = codec.toMessage(new EmailTask("order-1:SHIPPED", "a@b.c", "s", "b"));

        assertThat(message.getMessageProperties().getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
        assertThat(message.getMessageProperties().getContentType()).isEqualTo("application/json");
        assertThat(message.getMessageProperties().getMessageId()).isEqualTo("order-1:SHIPPED");
        assertThat(message.getMessageProperties().getHeaders()).doesNotContainKey("__TypeId__");
    }
}
