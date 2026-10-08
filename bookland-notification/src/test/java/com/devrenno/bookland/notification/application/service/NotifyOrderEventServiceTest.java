package com.devrenno.bookland.notification.application.service;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.dto.NotifyOutcome;
import com.devrenno.bookland.notification.application.port.out.EmailTaskQueuePort;
import com.devrenno.bookland.notification.domain.service.OrderEmailComposer;
import com.devrenno.bookland.notification.domain.valueobject.OrderEventKind;
import com.devrenno.bookland.notification.domain.valueobject.OrderNotice;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class NotifyOrderEventServiceTest {

    @Mock private EmailTaskQueuePort emailTaskQueuePort;

    private NotifyOrderEventService service;

    private final UUID orderId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = NotifyOrderEventService.create(new OrderEmailComposer(), emailTaskQueuePort);
    }

    /** The key is what lets the sender recognise the same email delivered twice (step 6c). */
    @Test
    void queuesTheWrittenEmail_keyedByOrderAndKind() {
        assertThat(service.notify(notice("reader@bookland.com"))).isEqualTo(NotifyOutcome.QUEUED);

        ArgumentCaptor<EmailTask> task = ArgumentCaptor.forClass(EmailTask.class);
        verify(emailTaskQueuePort).enqueue(task.capture());
        assertThat(task.getValue().key()).isEqualTo(orderId + ":CONFIRMED");
        assertThat(task.getValue().to()).isEqualTo("reader@bookland.com");
        assertThat(task.getValue().subject()).contains("confirmed");
        assertThat(task.getValue().body()).contains("Total: 10.00");
    }

    @Test
    void noEmailAddress_queuesNothing() {
        assertThat(service.notify(notice(null))).isEqualTo(NotifyOutcome.NO_RECIPIENT);
        verifyNoInteractions(emailTaskQueuePort);
    }

    /** The listener must see the failure, or the Kafka offset would say the event was handled. */
    @Test
    void aQueueThatDoesNotConfirm_failsTheCall() {
        doThrow(new IllegalStateException("no confirm")).when(emailTaskQueuePort).enqueue(any());

        assertThatThrownBy(() -> service.notify(notice("reader@bookland.com")))
                .isInstanceOf(IllegalStateException.class);
    }

    private OrderNotice notice(String email) {
        return new OrderNotice(orderId, OrderEventKind.CONFIRMED, email, "Ana", new BigDecimal("10.00"),
                List.of(), null);
    }
}
