package com.devrenno.bookland.catalog.infrastructure.persistence.adapter;

import com.devrenno.bookland.catalog.application.dto.CategoryResponse;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.domain.entity.Category;
import com.devrenno.bookland.catalog.infrastructure.persistence.mapper.CatalogPersistenceMapper;
import com.devrenno.bookland.catalog.infrastructure.persistence.repository.CategoryJpaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class CategoryPersistenceAdapter implements CategoryPersistencePort {

    private final CategoryJpaRepository categoryRepository;
    private final CatalogPersistenceMapper mapper;

    @Override
    public Optional<Category> findById(UUID id) {
        return categoryRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<CategoryResponse> findAllActiveWithBookCount() {
        return categoryRepository.findAllActiveWithBookCount().stream()
                .map(row -> new CategoryResponse(
                        (UUID) row[0],
                        (String) row[1],
                        ((Number) row[2]).longValue()
                ))
                .toList();
    }
}
