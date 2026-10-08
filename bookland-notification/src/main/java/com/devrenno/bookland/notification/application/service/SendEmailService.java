package com.devrenno.bookland.notification.application.service;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.port.in.SendEmailUseCase;
import com.devrenno.bookland.notification.application.port.out.MailSenderPort;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;

/** Hands a queued email to the mail server. A failure propagates, so the queue can try again. */
public class SendEmailService implements SendEmailUseCase {

    private final MailSenderPort mailSenderPort;

    private SendEmailService(MailSenderPort mailSenderPort) {
        this.mailSenderPort = mailSenderPort;
    }

    public static SendEmailService create(MailSenderPort mailSenderPort) {
        return new SendEmailService(mailSenderPort);
    }

    @Override
    public void send(EmailTask task) {
        mailSenderPort.send(new EmailMessage(task.to(), task.subject(), task.body()));
    }
}
