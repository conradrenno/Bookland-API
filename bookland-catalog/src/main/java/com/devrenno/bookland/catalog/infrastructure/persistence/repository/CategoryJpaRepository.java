package com.devrenno.bookland.catalog.infrastructure.persistence.repository;

import com.devrenno.bookland.catalog.infrastructure.persistence.entity.CategoryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.UUID;

public interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {

    @Query("""
            SELECT c.id, c.name, COUNT(b.id)
            FROM CategoryJpaEntity c
            LEFT JOIN BookJpaEntity b ON b.category.id = c.id AND b.active = true
            WHERE c.active = true
            GROUP BY c.id, c.name
            ORDER BY c.name
            """)
    List<Object[]> findAllActiveWithBookCount();
}
