package com.devrenno.bookland.notification.application.port.in;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.dto.SendOutcome;

/** Delivers one email taken from the task queue, once per key whatever the number of deliveries. */
public interface SendEmailUseCase {

    SendOutcome send(EmailTask task);
}
