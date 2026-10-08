package com.devrenno.bookland.notification.infrastructure.mail;

import com.devrenno.bookland.notification.application.port.out.MailSenderPort;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Sends over SMTP through Spring's {@link JavaMailSender}, which Boot builds from
 * {@code spring.mail.*}. In dev and in the compose stack the server is Mailpit, which keeps every
 * email and shows it on a web page instead of delivering it. A refused or unreachable server is a
 * {@code MailException}, left to propagate.
 */
@Component
public class SmtpMailSenderAdapter implements MailSenderPort {

    private final JavaMailSender mailSender;
    private final String from;

    public SmtpMailSenderAdapter(JavaMailSender mailSender,
                                 @Value("${bookland.notification.mail.from}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    @Override
    public void send(EmailMessage message) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(from);
        mail.setTo(message.to());
        mail.setSubject(message.subject());
        mail.setText(message.body());
        mailSender.send(mail);
    }
}
