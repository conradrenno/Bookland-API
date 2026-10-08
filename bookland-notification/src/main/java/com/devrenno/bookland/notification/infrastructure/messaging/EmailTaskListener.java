package com.devrenno.bookland.notification.infrastructure.messaging;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.port.in.SendEmailUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * Takes email tasks off the queue and sends them — the RabbitMQ twin of the Kafka listener. The
 * task is acknowledged when this method returns, that is, once the mail server took the email; an
 * exception rejects it (see {@link NotificationRabbitConfig}'s factory for what happens next).
 */
@Component
public class EmailTaskListener {

    private static final Logger log = LoggerFactory.getLogger(EmailTaskListener.class);

    private final SendEmailUseCase sendEmailUseCase;
    private final EmailTaskCodec codec;

    public EmailTaskListener(SendEmailUseCase sendEmailUseCase, EmailTaskCodec codec) {
        this.sendEmailUseCase = sendEmailUseCase;
        this.codec = codec;
    }

    @RabbitListener(queues = NotificationRabbitConfig.EMAIL_QUEUE,
            containerFactory = NotificationRabbitConfig.LISTENER_CONTAINER_FACTORY)
    public void on(Message message) {
        EmailTask task = codec.fromMessage(message);
        try {
            sendEmailUseCase.send(task);
        } catch (RuntimeException e) {
            log.error("Email {} not sent, task rejected: {}", task.key(), e.getMessage());
            throw e;
        }
        log.info("Email {} sent", task.key());
    }
}
