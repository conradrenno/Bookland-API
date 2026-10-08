package com.devrenno.bookland.notification.infrastructure.persistence;

import com.devrenno.bookland.notification.application.port.out.SentEmailPort;
import org.springframework.stereotype.Repository;

import java.time.Instant;

/**
 * The {@code sent_emails} table. Each call is its own transaction (the repository's): the record
 * is written right after the mail server took the email, and there is nothing else to commit with it.
 *
 * <p>Check-then-insert is safe here because one consumer takes the email queue one task at a time
 * (the listener container's default concurrency of 1); the primary key would still refuse a second
 * row if that ever changed.
 */
@Repository
public class SentEmailPersistenceAdapter implements SentEmailPort {

    private final SentEmailJpaRepository repository;

    public SentEmailPersistenceAdapter(SentEmailJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean wasSent(String key) {
        return repository.existsById(key);
    }

    @Override
    public void recordSent(String key, String recipient, String subject) {
        repository.save(new SentEmailJpaEntity(key, recipient, subject, Instant.now()));
    }
}
