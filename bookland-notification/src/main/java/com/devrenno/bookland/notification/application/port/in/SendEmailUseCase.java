package com.devrenno.bookland.notification.application.port.in;

import com.devrenno.bookland.notification.application.dto.EmailTask;

/** Delivers one email taken from the task queue. */
public interface SendEmailUseCase {

    void send(EmailTask task);
}
