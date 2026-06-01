package com.devrenno.bookland.catalog.infrastructure.persistence.repository;

import com.devrenno.bookland.catalog.infrastructure.persistence.entity.BookJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

public interface BookJpaRepository extends JpaRepository<BookJpaEntity, UUID>, JpaSpecificationExecutor<BookJpaEntity> {

    Optional<BookJpaEntity> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    Page<BookJpaEntity> findByCategory_IdAndActiveTrue(UUID categoryId, Pageable pageable);
}
