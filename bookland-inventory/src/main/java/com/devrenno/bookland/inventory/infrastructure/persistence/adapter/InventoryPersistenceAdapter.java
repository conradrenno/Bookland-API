package com.devrenno.bookland.inventory.infrastructure.persistence.adapter;

import com.devrenno.bookland.inventory.application.common.PageQuery;
import com.devrenno.bookland.inventory.application.common.PageResult;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;
import com.devrenno.bookland.inventory.infrastructure.persistence.mapper.InventoryPersistenceMapper;
import com.devrenno.bookland.inventory.infrastructure.persistence.repository.InventoryEntryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class InventoryPersistenceAdapter implements InventoryPersistencePort {

    private final InventoryEntryJpaRepository repository;
    private final InventoryPersistenceMapper mapper;

    @Override
    public InventoryEntry save(InventoryEntry entry) {
        return mapper.toDomain(repository.save(mapper.toEntity(entry)));
    }

    @Override
    public PageResult<InventoryEntry> findByBookId(UUID bookId, PageQuery pageQuery) {
        Page<InventoryEntry> page = repository
                .findByBookIdOrderByAdjustedAtDesc(bookId, PageRequest.of(pageQuery.page(), pageQuery.size()))
                .map(mapper::toDomain);
        return new PageResult<>(
                page.getContent(), page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages()
        );
    }

    @Override
    public Optional<Instant> findLastAdjustmentTime(UUID bookId) {
        return repository.findFirstByBookIdOrderByAdjustedAtDesc(bookId)
                .map(e -> e.getAdjustedAt());
    }
}
