package com.devrenno.bookland.wishlist.infrastructure.persistence.adapter;

import com.devrenno.bookland.wishlist.application.port.out.WishlistPersistencePort;
import com.devrenno.bookland.wishlist.domain.entity.Wishlist;
import com.devrenno.bookland.wishlist.domain.entity.WishlistItem;
import com.devrenno.bookland.wishlist.infrastructure.persistence.entity.WishlistItemJpaEntity;
import com.devrenno.bookland.wishlist.infrastructure.persistence.entity.WishlistJpaEntity;
import com.devrenno.bookland.wishlist.infrastructure.persistence.repository.WishlistJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class WishlistPersistenceAdapter implements WishlistPersistencePort {

    private final WishlistJpaRepository wishlistRepository;

    @Override
    public Wishlist save(Wishlist wishlist) {
        WishlistJpaEntity entity = wishlistRepository.findByCustomerId(wishlist.getCustomerId())
                .orElseGet(() -> WishlistJpaEntity.builder()
                        .id(wishlist.getId())
                        .customerId(wishlist.getCustomerId())
                        .build());

        entity.getItems().clear();
        List<WishlistItemJpaEntity> itemEntities = wishlist.getItems().stream()
                .map(i -> WishlistItemJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .wishlist(entity)
                        .bookId(i.getBookId())
                        .addedAt(i.getAddedAt())
                        .build())
                .toList();
        entity.getItems().addAll(itemEntities);

        return toDomain(wishlistRepository.save(entity));
    }

    @Override
    public Optional<Wishlist> findByCustomerId(UUID customerId) {
        return wishlistRepository.findByCustomerId(customerId).map(this::toDomain);
    }

    private Wishlist toDomain(WishlistJpaEntity entity) {
        List<WishlistItem> items = entity.getItems().stream()
                .map(i -> new WishlistItem(i.getBookId(), i.getAddedAt()))
                .toList();
        return Wishlist.builder()
                .id(entity.getId())
                .customerId(entity.getCustomerId())
                .items(new ArrayList<>(items))
                .build();
    }
}
