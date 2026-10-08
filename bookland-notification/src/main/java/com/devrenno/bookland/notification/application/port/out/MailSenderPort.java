package com.devrenno.bookland.notification.application.port.out;

import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;

/** The mail server. Throws when it does not take the message. */
public interface MailSenderPort {

    void send(EmailMessage message);
}
