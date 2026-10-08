package com.devrenno.bookland;

import com.devrenno.bookland.notification.application.dto.EmailTask;
import com.devrenno.bookland.notification.application.port.out.EmailTaskQueuePort;
import com.devrenno.bookland.notification.application.port.out.MailSenderPort;
import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import com.devrenno.bookland.notification.infrastructure.messaging.EmailTaskCodec;
import com.devrenno.bookland.notification.infrastructure.messaging.EmailTaskListener;
import com.devrenno.bookland.notification.infrastructure.messaging.EmailTaskRedelivery;
import org.springframework.amqp.core.Message;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * What a test cannot run in the JVM, replaced at its seam.
 *
 * <ul>
 *   <li>{@link InMemoryEmailQueue} — the RabbitMQ queue. It encodes the task with the real codec and
 *       hands the message straight to the real {@link EmailTaskListener}, so everything but the
 *       broker itself runs. It can be told to refuse the next tasks, as an unreachable broker
 *       would.</li>
 *   <li>{@link InMemoryRedelivery} — the wait queues and the dead-letter queue. A retry is delivered
 *       again at once, with the try number the real one would carry, and its delay recorded instead
 *       of waited; a task given up is kept in a list.</li>
 *   <li>{@link RecordingMailSender} — the SMTP server: it keeps what it was given, and can be told
 *       to fail for an address.</li>
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
    public InMemoryRedelivery inMemoryRedelivery(EmailTaskCodec codec, ObjectProvider<EmailTaskListener> listener) {
        return new InMemoryRedelivery(codec, listener);
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

    public static class InMemoryRedelivery implements EmailTaskRedelivery {

        private final EmailTaskCodec codec;
        private final ObjectProvider<EmailTaskListener> listener;
        private final Map<String, List<Duration>> waits = new ConcurrentHashMap<>();
        private final List<Message> deadLetters = new CopyOnWriteArrayList<>();

        InMemoryRedelivery(EmailTaskCodec codec, ObjectProvider<EmailTaskListener> listener) {
            this.codec = codec;
            this.listener = listener;
        }

        @Override
        public void retryLater(Message failed, int nextAttempt, Duration delay, String reason) {
            waits.computeIfAbsent(codec.fromMessage(failed).key(), key -> new CopyOnWriteArrayList<>()).add(delay);
            listener.getObject().on(codec.withAttempt(failed, nextAttempt, reason));
        }

        @Override
        public void giveUp(Message failed, int attempts, String reason) {
            deadLetters.add(codec.withAttempt(failed, attempts, reason));
        }

        /** The delays a task waited, in order. */
        public List<Duration> waitsOf(String key) {
            return waits.getOrDefault(key, List.of());
        }

        public List<Message> deadLettersOf(String key) {
            return deadLetters.stream().filter(message -> codec.fromMessage(message).key().equals(key)).toList();
        }
    }

    public static class RecordingMailSender implements MailSenderPort {

        private final List<EmailMessage> sent = new CopyOnWriteArrayList<>();
        private final Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();

        /** The next {@code times} emails to this address fail, as an unreachable mail server would. */
        public void failNext(String address, int times) {
            failures.put(address, new AtomicInteger(times));
        }

        @Override
        public void send(EmailMessage message) {
            AtomicInteger left = failures.get(message.to());
            if (left != null && left.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
                throw new IllegalStateException("Simulated: mail server unreachable");
            }
            sent.add(message);
        }

        public List<EmailMessage> sentTo(String address) {
            return sent.stream().filter(message -> message.to().equals(address)).toList();
        }
    }
}
