package com.devrenno.bookland.notification.application.service;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.dto.SendOutcome;
import com.devrenno.bookland.notification.application.port.out.MailSenderPort;
import com.devrenno.bookland.notification.application.port.out.SentEmailPort;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SendEmailServiceTest {

    @Mock private MailSenderPort mailSenderPort;
    @Mock private SentEmailPort sentEmailPort;

    private SendEmailService service;

    private final EmailTask task = new EmailTask("order-1:CONFIRMED", "reader@bookland.com", "Confirmed", "Hello");

    @BeforeEach
    void setUp() {
        service = SendEmailService.create(mailSenderPort, sentEmailPort);
    }

    @Test
    void sendsAndRecords() {
        assertThat(service.send(task)).isEqualTo(SendOutcome.SENT);

        verify(mailSenderPort).send(new EmailMessage("reader@bookland.com", "Confirmed", "Hello"));
        verify(sentEmailPort).recordSent("order-1:CONFIRMED", "reader@bookland.com", "Confirmed");
    }

    /** The same email reaching the sender twice — a duplicated task, or a retry after a send that went through. */
    @Test
    void anEmailSentBefore_isNotSentAgain() {
        when(sentEmailPort.wasSent("order-1:CONFIRMED")).thenReturn(true);

        assertThat(service.send(task)).isEqualTo(SendOutcome.ALREADY_SENT);

        verifyNoInteractions(mailSenderPort);
        verify(sentEmailPort, never()).recordSent(anyString(), anyString(), anyString());
    }

    /** Recorded only after the mail server took it: a failed send must stay eligible for the retry. */
    @Test
    void aFailedSend_isNotRecorded() {
        doThrow(new IllegalStateException("SMTP down")).when(mailSenderPort).send(any());

        assertThatThrownBy(() -> service.send(task)).isInstanceOf(IllegalStateException.class);

        verify(sentEmailPort, never()).recordSent(anyString(), anyString(), anyString());
    }
}
