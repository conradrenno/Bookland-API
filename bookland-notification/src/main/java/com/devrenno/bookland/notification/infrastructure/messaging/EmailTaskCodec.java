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
 * producer's class name in a header ({@code __TypeId__}). Publisher, listener and redelivery use this
 * one class, so they cannot drift apart.
 *
 * <p>Two headers of ours ride along once a task has failed: which try comes next, and why the last
 * one failed — what a person looking at the dead-letter queue needs to know.
 */
@Component
public class EmailTaskCodec {

    static final String ATTEMPT_HEADER = "x-bookland-attempt";
    static final String LAST_ERROR_HEADER = "x-bookland-last-error";
    private static final int MAX_ERROR_LENGTH = 500;

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

    /** Which try this delivery is: 1 for a task fresh from the publisher. */
    public int attemptOf(Message message) {
        Object attempt = message.getMessageProperties().getHeaders().get(ATTEMPT_HEADER);
        return attempt instanceof Number number ? number.intValue() : 1;
    }

    /** A copy of the message, same body, marked as the given try and with the last failure. */
    public Message withAttempt(Message message, int attempt, String lastError) {
        String error = lastError == null ? "" : lastError;
        return MessageBuilder.fromMessage(message)
                .setHeader(ATTEMPT_HEADER, attempt)
                .setHeader(LAST_ERROR_HEADER, error.length() > MAX_ERROR_LENGTH ? error.substring(0, MAX_ERROR_LENGTH) : error)
                .setDeliveryMode(MessageDeliveryMode.PERSISTENT)
                .build();
    }
}
