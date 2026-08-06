package com.devrenno.bookland.catalog.infrastructure.persistence.repository;

import com.devrenno.bookland.catalog.infrastructure.persistence.entity.BookJpaEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface BookJpaRepository extends JpaRepository<BookJpaEntity, UUID>, JpaSpecificationExecutor<BookJpaEntity> {

    /**
     * Conditional decrement: the store re-evaluates {@code stock_quantity >= :quantity} after taking
     * the row lock, so a concurrent checkout that already consumed the units updates 0 rows instead
     * of overwriting them with a stale absolute value.
     *
     * <p>{@code flushAutomatically} pushes pending inserts (the payment row) out before the bulk
     * update; {@code clearAutomatically} then detaches the persistence context, so a book read
     * earlier in the same transaction is not handed back with the pre-decrement stock.</p>
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update BookJpaEntity b
               set b.stockQuantity = b.stockQuantity - :quantity,
                   b.updatedAt = :now
             where b.id = :id
               and b.active = true
               and b.stockQuantity >= :quantity
            """)
    int decrementSellableStock(@Param("id") UUID id, @Param("quantity") int quantity, @Param("now") Instant now);

    /**
     * The same guard without the {@code active} filter, for administrative corrections: a delisted
     * book must not be sold, but its count must still be correctable.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update BookJpaEntity b
               set b.stockQuantity = b.stockQuantity - :quantity,
                   b.updatedAt = :now
             where b.id = :id
               and b.stockQuantity >= :quantity
            """)
    int decrementStock(@Param("id") UUID id, @Param("quantity") int quantity, @Param("now") Instant now);

    /**
     * Unconditional relative increment, deliberately without the {@code active} filter that guards
     * the decrement: a delisted book must not be <em>sold</em>, but units coming back from a
     * cancelled order are still units, and dropping them would leave the count wrong for good if
     * the book is ever relisted.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update BookJpaEntity b
               set b.stockQuantity = b.stockQuantity + :quantity,
                   b.updatedAt = :now
             where b.id = :id
            """)
    int incrementStock(@Param("id") UUID id, @Param("quantity") int quantity, @Param("now") Instant now);

    Optional<BookJpaEntity> findByIsbn(String isbn);

    boolean existsByIsbn(String isbn);

    Page<BookJpaEntity> findByCategory_IdAndActiveTrue(UUID categoryId, Pageable pageable);

    Page<BookJpaEntity> findByStockQuantityLessThanEqualAndActiveTrue(int threshold, Pageable pageable);
}
