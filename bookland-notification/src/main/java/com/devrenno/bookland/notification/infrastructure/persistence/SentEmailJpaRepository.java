package com.devrenno.bookland.notification.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface SentEmailJpaRepository extends JpaRepository<SentEmailJpaEntity, String> {
}
