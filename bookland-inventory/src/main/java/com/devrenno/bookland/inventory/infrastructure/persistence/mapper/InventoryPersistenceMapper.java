package com.devrenno.bookland.inventory.infrastructure.persistence.mapper;

import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;
import com.devrenno.bookland.inventory.infrastructure.persistence.entity.InventoryEntryJpaEntity;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface InventoryPersistenceMapper {

    InventoryEntryJpaEntity toEntity(InventoryEntry entry);

    default InventoryEntry toDomain(InventoryEntryJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return InventoryEntry.reconstitute(
                entity.getId(),
                entity.getBookId(),
                entity.getPreviousQuantity(),
                entity.getNewQuantity(),
                entity.getDelta(),
                entity.getReason(),
                entity.getAdjustedBy(),
                entity.getAdjustedAt()
        );
    }
}
