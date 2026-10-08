package com.devrenno.bookland.notification.application.service;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.dto.NotifyOutcome;
import com.devrenno.bookland.notification.application.port.in.NotifyOrderEventUseCase;
import com.devrenno.bookland.notification.application.port.out.EmailTaskQueuePort;
import com.devrenno.bookland.notification.domain.service.OrderEmailComposer;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import com.devrenno.bookland.notification.domain.valueobject.OrderNotice;

import java.util.Optional;

/**
 * Writes the email when the event arrives and queues it as a task, instead of sending it here: the
 * mail server may be down for minutes, and waiting for it would hold up every event behind this one
 * on the Kafka partition. The queue takes over from here, retries included.
 */
public class NotifyOrderEventService implements NotifyOrderEventUseCase {

    private final OrderEmailComposer composer;
    private final EmailTaskQueuePort emailTaskQueuePort;

    private NotifyOrderEventService(OrderEmailComposer composer, EmailTaskQueuePort emailTaskQueuePort) {
        this.composer = composer;
        this.emailTaskQueuePort = emailTaskQueuePort;
    }

    public static NotifyOrderEventService create(OrderEmailComposer composer, EmailTaskQueuePort emailTaskQueuePort) {
        return new NotifyOrderEventService(composer, emailTaskQueuePort);
    }

    @Override
    public NotifyOutcome notify(OrderNotice notice) {
        Optional<EmailMessage> email = composer.compose(notice);
        if (email.isEmpty()) {
            return NotifyOutcome.NO_RECIPIENT;
        }
        EmailMessage message = email.get();
        emailTaskQueuePort.enqueue(new EmailTask(notice.orderId() + ":" + notice.kind(),
                message.to(), message.subject(), message.body()));
        return NotifyOutcome.QUEUED;
    }
}
