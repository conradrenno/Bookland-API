package com.devrenno.bookland.notification.infrastructure.messaging;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.dto.SendOutcome;
import com.devrenno.bookland.notification.application.port.in.SendEmailUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.core.Message;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailTaskListenerTest {

    @Mock private SendEmailUseCase sendEmailUseCase;
    @Mock private EmailTaskRedelivery redelivery;

    private final EmailTaskCodec codec = new EmailTaskCodec(JsonMapper.builder().build());
    private final EmailTask task = new EmailTask("order-1:CONFIRMED", "reader@bookland.com", "Confirmed", "Hello");
    private EmailTaskListener listener;

    @BeforeEach
    void setUp() {
        listener = new EmailTaskListener(sendEmailUseCase, codec, redelivery, new NotificationRetryProperties(null));
    }

    @Test
    void sent_isAcknowledgedWithNothingElseToDo() {
        when(sendEmailUseCase.send(task)).thenReturn(SendOutcome.SENT);

        listener.on(codec.toMessage(task));

        verifyNoInteractions(redelivery);
    }

    @Test
    void theFirstFailure_waitsTenSecondsAsTryTwo() {
        when(sendEmailUseCase.send(task)).thenThrow(new IllegalStateException("SMTP down"));
        Message message = codec.toMessage(task);

        listener.on(message);

        verify(redelivery).retryLater(eq(message), eq(2), eq(Duration.ofSeconds(10)), contains("SMTP down"));
    }

    @Test
    void theThirdFailure_waitsFiveMinutes() {
        when(sendEmailUseCase.send(task)).thenThrow(new IllegalStateException("SMTP down"));

        listener.on(codec.withAttempt(codec.toMessage(task), 3, "earlier"));

        verify(redelivery).retryLater(any(), eq(4), eq(Duration.ofMinutes(5)), any());
    }

    /** Three delays: the fourth try is the last one. */
    @Test
    void theFourthFailure_givesUp() {
        when(sendEmailUseCase.send(task)).thenThrow(new IllegalStateException("SMTP down"));

        listener.on(codec.withAttempt(codec.toMessage(task), 4, "earlier"));

        verify(redelivery).giveUp(any(), eq(4), contains("SMTP down"));
        verify(redelivery, never()).retryLater(any(), anyInt(), any(), any());
    }

    /** Not a task at all: thrown, so the container rejects it and the queue dead-letters it. */
    @Test
    void aBodyThatIsNotATask_isThrownForTheDeadLetterRoute() {
        Message garbage = new Message("not json".getBytes(StandardCharsets.UTF_8));

        assertThatThrownBy(() -> listener.on(garbage)).isInstanceOf(RuntimeException.class);
        verifyNoInteractions(sendEmailUseCase, redelivery);
    }

    /** The broker would not take the retry: thrown, so the original is dead-lettered rather than lost. */
    @Test
    void aRetryTheBrokerRefuses_isThrown() {
        when(sendEmailUseCase.send(task)).thenThrow(new IllegalStateException("SMTP down"));
        org.mockito.Mockito.doThrow(new IllegalStateException("no confirm"))
                .when(redelivery).retryLater(any(), anyInt(), any(), any());

        assertThatThrownBy(() -> listener.on(codec.toMessage(task))).hasMessage("no confirm");
    }
}
