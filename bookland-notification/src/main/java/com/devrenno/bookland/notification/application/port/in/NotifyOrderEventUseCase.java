package com.devrenno.bookland.notification.application.port.in;

import com.devrenno.bookland.notification.application.dto.NotifyOutcome;
import com.devrenno.bookland.notification.domain.valueobject.OrderNotice;

/** Turns an order event into an email task on the queue. Does not send anything itself. */
public interface NotifyOrderEventUseCase {

    NotifyOutcome notify(OrderNotice notice);
}
