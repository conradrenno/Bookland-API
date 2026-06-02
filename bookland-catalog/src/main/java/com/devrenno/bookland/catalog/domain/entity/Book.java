package com.devrenno.bookland.catalog.domain.entity;

import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;
import com.devrenno.bookland.catalog.domain.valueobject.BookId;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
public class Book {

    private final BookId id;
    private String title;
    private final ISBN isbn;
    private List<String> authors;
    private String publisher;
    private Integer publicationYear;
    private String edition;
    private String synopsis;
    private Price price;
    private int stockQuantity;
    private CategoryId categoryId;
    private double avgRating;
    private boolean active;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static Book create(
            String title,
            ISBN isbn,
            List<String> authors,
            String publisher,
            Integer publicationYear,
            String edition,
            String synopsis,
            Price price,
            int stockQuantity,
            CategoryId categoryId
    ) {
        return Book.builder()
                .id(BookId.generate())
                .title(title)
                .isbn(isbn)
                .authors(authors)
                .publisher(publisher)
                .publicationYear(publicationYear)
                .edition(edition)
                .synopsis(synopsis)
                .price(price)
                .stockQuantity(stockQuantity)
                .categoryId(categoryId)
                .avgRating(0.0)
                .active(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void update(
            String title,
            List<String> authors,
            String publisher,
            Integer publicationYear,
            String edition,
            String synopsis,
            Price price,
            int stockQuantity,
            CategoryId categoryId
    ) {
        if (title != null) this.title = title;
        if (authors != null) this.authors = authors;
        if (publisher != null) this.publisher = publisher;
        if (publicationYear != null) this.publicationYear = publicationYear;
        if (edition != null) this.edition = edition;
        if (synopsis != null) this.synopsis = synopsis;
        if (price != null) this.price = price;
        if (stockQuantity >= 0) this.stockQuantity = stockQuantity;
        if (categoryId != null) this.categoryId = categoryId;
        this.updatedAt = LocalDateTime.now();
    }

    public void adjustStock(int delta) {
        int newQty = this.stockQuantity + delta;
        if (newQty < 0) {
            throw new InsufficientStockException(this.id.value(), this.stockQuantity, delta);
        }
        this.stockQuantity = newQty;
        this.updatedAt = LocalDateTime.now();
    }

    public void updateAverageRating(double newAvgRating) {
        this.avgRating = newAvgRating;
        this.updatedAt = LocalDateTime.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = LocalDateTime.now();
    }
}
