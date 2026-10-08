package com.devrenno.bookland.notification.infrastructure.messaging;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageBuilder;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;

/**
 * How an email task is written to and read from the queue: JSON in the body, written with
 * {@link JsonMapper} — never Spring AMQP's JSON converter, which, like Spring Kafka's, puts the
 * producer's class name in a header ({@code __TypeId__}). Publisher and listener use this one class,
 * so they cannot drift apart.
 */
@Component
public class EmailTaskCodec {

    private final JsonMapper jsonMapper;

    public EmailTaskCodec(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    /** Persistent, so a task the broker confirmed survives the broker's restart. */
    public Message toMessage(EmailTask task) {
        return MessageBuilder.withBody(jsonMapper.writeValueAsString(task).getBytes(StandardCharsets.UTF_8))
                .setContentType(MessageProperties.CONTENT_TYPE_JSON)
                .setContentEncoding(StandardCharsets.UTF_8.name())
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .setMessageId(task.key())
                .build();
    }

    public EmailTask fromMessage(Message message) {
        return jsonMapper.readValue(new String(message.getBody(), StandardCharsets.UTF_8), EmailTask.class);
    }
}
