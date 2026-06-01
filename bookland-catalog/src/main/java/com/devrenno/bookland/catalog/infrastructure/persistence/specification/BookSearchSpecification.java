package com.devrenno.bookland.catalog.infrastructure.persistence.specification;

import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.infrastructure.persistence.entity.BookJpaEntity;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public class BookSearchSpecification {

    private BookSearchSpecification() {}

    public static Specification<BookJpaEntity> from(BookSearchQuery query) {
        return (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.isTrue(root.get("active")));

            if (query.q() != null && !query.q().isBlank()) {
                String pattern = "%" + query.q().toLowerCase() + "%";
                Predicate titleMatch = cb.like(cb.lower(root.get("title")), pattern);
                jakarta.persistence.criteria.Join<BookJpaEntity, String> authorJoin =
                        root.join("authors", JoinType.LEFT);
                Predicate authorMatch = cb.like(cb.lower(authorJoin.as(String.class)), pattern);
                predicates.add(cb.or(titleMatch, authorMatch));
                cq.distinct(true);
            }

            if (query.categoryId() != null) {
                predicates.add(cb.equal(root.get("category").get("id"), query.categoryId()));
            }

            if (query.minPrice() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("price"), query.minPrice()));
            }

            if (query.maxPrice() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("price"), query.maxPrice()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
