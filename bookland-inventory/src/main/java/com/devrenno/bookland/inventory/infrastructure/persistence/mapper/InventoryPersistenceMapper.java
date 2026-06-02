package com.devrenno.bookland.inventory.infrastructure.persistence.mapper;

import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;
import com.devrenno.bookland.inventory.infrastructure.persistence.entity.InventoryEntryJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryPersistenceMapper {

    InventoryEntryJpaEntity toEntity(InventoryEntry entry);

    InventoryEntry toDomain(InventoryEntryJpaEntity entity);
}
