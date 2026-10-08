package com.devrenno.bookland;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.port.out.EmailTaskQueuePort;
import com.devrenno.bookland.notification.application.port.out.MailSenderPort;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import com.devrenno.bookland.notification.infrastructure.messaging.EmailTaskCodec;
import com.devrenno.bookland.notification.infrastructure.messaging.EmailTaskListener;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The two things a test cannot run in the JVM, replaced at their ports.
 *
 * <ul>
 *   <li>{@link InMemoryEmailQueue} — the RabbitMQ queue. It encodes the task with the real codec
 *       and hands the message straight to the real {@link EmailTaskListener}, so everything but the
 *       broker itself runs. It can be told to refuse the next tasks, as an unreachable broker
 *       would.</li>
 *   <li>{@link RecordingMailSender} — the SMTP server: it keeps what it was given.</li>
 * </ul>
 */
@TestConfiguration
public class NotificationTestDoubles {

    @Bean
    @Primary
    public InMemoryEmailQueue inMemoryEmailQueue(EmailTaskCodec codec, ObjectProvider<EmailTaskListener> listener) {
        return new InMemoryEmailQueue(codec, listener);
    }

    @Bean
    @Primary
    public RecordingMailSender recordingMailSender() {
        return new RecordingMailSender();
    }

    public static class InMemoryEmailQueue implements EmailTaskQueuePort {

        private final EmailTaskCodec codec;
        private final ObjectProvider<EmailTaskListener> listener;
        private final AtomicInteger refusals = new AtomicInteger();

        InMemoryEmailQueue(EmailTaskCodec codec, ObjectProvider<EmailTaskListener> listener) {
            this.codec = codec;
            this.listener = listener;
        }

        /** The next {@code times} tasks fail as a publish without the broker's confirmation would. */
        public void refuseNext(int times) {
            refusals.set(times);
        }

        @Override
        public void enqueue(EmailTask task) {
            if (refusals.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                throw new IllegalStateException("Simulated: the broker did not confirm the task");
            }
            listener.getObject().on(codec.toMessage(task));
        }
    }

    public static class RecordingMailSender implements MailSenderPort {

        private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();

        @Override
        public void send(EmailMessage message) {
            sent.add(message);
        }

        public List<EmailMessage> sentTo(String address) {
            return sent.stream().filter(message -> message.to().equals(address)).toList();
        }
    }
}
