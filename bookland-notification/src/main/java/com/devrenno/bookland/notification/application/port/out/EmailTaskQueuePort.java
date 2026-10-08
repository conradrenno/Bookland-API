package com.devrenno.bookland.notification.application.port.out;

import com.devrenno.bookland.notification.application.dto.EmailTask;

/**
 * The queue of emails waiting to be sent. {@link #enqueue} returns only once the queue has the task
 * for good; any failure to get that confirmation is an exception, so the caller does not treat the
 * event as handled.
 */
public interface EmailTaskQueuePort {

    void enqueue(EmailTask task);
}
