package com.devrenno.bookland.notification.infrastructure.messaging;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.dto.SendOutcome;
import com.devrenno.bookland.notification.application.port.in.SendEmailUseCase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;

/**
 * Takes email tasks off the queue and sends them — the RabbitMQ twin of the Kafka listener.
 *
 * <p>When the mail server fails, the task is not thrown back at the queue: the listener sends a copy
 * to wait for the next delay ({@link NotificationRetryProperties}) — or, once the delays are used up,
 * to the dead-letter queue — and then returns, which acknowledges the original. Every failed try is
 * a WARN with the time of the next one; giving up is an ERROR.
 *
 * <p>Only what cannot be handled that way is thrown: a body that is not a task, or a retry the
 * broker would not take. The container then rejects the message without requeue, and the email
 * queue's dead-letter exchange moves it to the dead-letter queue.
 */
@Component
public class EmailTaskListener {

    private static final Logger log = LoggerFactory.getLogger(EmailTaskListener.class);

    private final SendEmailUseCase sendEmailUseCase;
    private final EmailTaskCodec codec;
    private final EmailTaskRedelivery redelivery;
    private final List<Duration> delays;

    public EmailTaskListener(SendEmailUseCase sendEmailUseCase, EmailTaskCodec codec, EmailTaskRedelivery redelivery,
                             NotificationRetryProperties retry) {
        this.sendEmailUseCase = sendEmailUseCase;
        this.codec = codec;
        this.redelivery = redelivery;
        this.delays = retry.delays();
    }

    @RabbitListener(queues = NotificationRabbitConfig.EMAIL_QUEUE,
            containerFactory = NotificationRabbitConfig.LISTENER_CONTAINER_FACTORY)
    public void on(Message message) {
        EmailTask task = codec.fromMessage(message);
        int attempt = codec.attemptOf(message);
        SendOutcome outcome;
        try {
            outcome = sendEmailUseCase.send(task);
        } catch (RuntimeException e) {
            String reason = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (attempt <= delays.size()) {
                Duration delay = delays.get(attempt - 1);
                log.warn("Email {} not sent (try {} of {}), next try in {}: {}",
                        task.key(), attempt, delays.size() + 1, delay, reason);
                redelivery.retryLater(message, attempt + 1, delay, reason);
            } else {
                log.error("Email {} not sent after {} tries, moved to the dead-letter queue: {}",
                        task.key(), attempt, reason);
                redelivery.giveUp(message, attempt, reason);
            }
            return;
        }
        if (outcome == SendOutcome.ALREADY_SENT) {
            log.info("Email {} was sent before, duplicate task dropped", task.key());
        } else {
            log.info("Email {} sent (try {})", task.key(), attempt);
        }
    }
}
