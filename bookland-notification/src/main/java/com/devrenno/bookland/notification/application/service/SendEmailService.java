package com.devrenno.bookland.notification.application.service;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.dto.SendOutcome;
import com.devrenno.bookland.notification.application.port.in.SendEmailUseCase;
import com.devrenno.bookland.notification.application.port.out.MailSenderPort;
import com.devrenno.bookland.notification.application.port.out.SentEmailPort;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;

/**
 * Hands a queued email to the mail server, unless an email with the same key was sent already. A
 * failure of the mail server propagates, so the queue can try again later.
 *
 * <p>The record is written after the send, not before: written first, a mail server that then
 * fails would leave an email recorded as sent that nobody received. The price is the opposite gap —
 * a crash between the send and the record sends that email once more on the next try. No mail
 * server takes part in a transaction with our database, so one of the two gaps has to stay; a
 * repeated email is the one that harms nobody.
 */
public class SendEmailService implements SendEmailUseCase {

    private final MailSenderPort mailSenderPort;
    private final SentEmailPort sentEmailPort;

    private SendEmailService(MailSenderPort mailSenderPort, SentEmailPort sentEmailPort) {
        this.mailSenderPort = mailSenderPort;
        this.sentEmailPort = sentEmailPort;
    }

    public static SendEmailService create(MailSenderPort mailSenderPort, SentEmailPort sentEmailPort) {
        return new SendEmailService(mailSenderPort, sentEmailPort);
    }

    @Override
    public SendOutcome send(EmailTask task) {
        if (sentEmailPort.wasSent(task.key())) {
            return SendOutcome.ALREADY_SENT;
        }
        mailSenderPort.send(new EmailMessage(task.to(), task.subject(), task.body()));
        sentEmailPort.recordSent(task.key(), task.to(), task.subject());
        return SendOutcome.SENT;
    }
}
