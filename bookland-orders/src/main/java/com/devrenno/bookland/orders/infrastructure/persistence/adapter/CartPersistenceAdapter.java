package com.devrenno.bookland.orders.infrastructure.persistence.adapter;

import com.devrenno.bookland.orders.application.port.out.CartPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Cart;
import com.devrenno.bookland.orders.domain.entity.CartItem;
import com.devrenno.bookland.orders.infrastructure.persistence.entity.CartItemJpaEntity;
import com.devrenno.bookland.orders.infrastructure.persistence.entity.CartJpaEntity;
import com.devrenno.bookland.orders.infrastructure.persistence.repository.CartJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CartPersistenceAdapter implements CartPersistencePort {

    private final CartJpaRepository cartRepository;

    @Override
    public Cart save(Cart cart) {
        CartJpaEntity entity = cartRepository.findByCustomerId(cart.getCustomerId())
                .orElseGet(() -> CartJpaEntity.builder()
                        .id(cart.getId())
                        .customerId(cart.getCustomerId())
                        .createdAt(cart.getCreatedAt())
                        .build());

        entity.setUpdatedAt(cart.getUpdatedAt());
        entity.getItems().clear();

        List<CartItemJpaEntity> itemEntities = cart.getItems().stream()
                .map(i -> CartItemJpaEntity.builder()
                        .id(UUID.randomUUID())
                        .cart(entity)
                        .bookId(i.getBookId())
                        .quantity(i.getQuantity())
                        .unitPriceAtAddition(i.getUnitPriceAtAddition())
                        .build())
                .toList();
        entity.getItems().addAll(itemEntities);

        CartJpaEntity saved = cartRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Cart> findByCustomerId(UUID customerId) {
        return cartRepository.findByCustomerId(customerId).map(this::toDomain);
    }

    @Override
    @Transactional
    public void deleteByCustomerId(UUID customerId) {
        cartRepository.deleteByCustomerId(customerId);
    }

    private Cart toDomain(CartJpaEntity entity) {
        List<CartItem> items = entity.getItems().stream()
                .map(i -> CartItem.of(i.getBookId(), i.getQuantity(), i.getUnitPriceAtAddition()))
                .toList();
        return Cart.reconstitute(
                entity.getId(), entity.getCustomerId(), items,
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
