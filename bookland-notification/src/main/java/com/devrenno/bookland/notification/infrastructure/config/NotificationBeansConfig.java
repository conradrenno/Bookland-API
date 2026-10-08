package com.devrenno.bookland.notification.infrastructure.config;

import com.devrenno.bookland.notification.application.port.in.NotifyOrderEventUseCase;
import com.devrenno.bookland.notification.application.port.in.SendEmailUseCase;
import com.devrenno.bookland.notification.application.port.out.EmailTaskQueuePort;
import com.devrenno.bookland.notification.application.port.out.MailSenderPort;
import com.devrenno.bookland.notification.application.service.NotifyOrderEventService;
import com.devrenno.bookland.notification.application.service.SendEmailService;
import com.devrenno.bookland.notification.domain.service.OrderEmailComposer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the notification module. It has no HTTP side, so no internal controller: its
 * two entry points are the use cases its own listeners drive — one per broker.
 */
@Configuration
public class NotificationBeansConfig {

    /** Driven by the OrderEventListener (Kafka). */
    @Bean
    public NotifyOrderEventUseCase notifyOrderEventUseCase(EmailTaskQueuePort emailTaskQueuePort) {
        return NotifyOrderEventService.create(new OrderEmailComposer(), emailTaskQueuePort);
    }

    /** Driven by the EmailTaskListener (RabbitMQ). */
    @Bean
    public SendEmailUseCase sendEmailUseCase(MailSenderPort mailSenderPort) {
        return SendEmailService.create(mailSenderPort);
    }
}
