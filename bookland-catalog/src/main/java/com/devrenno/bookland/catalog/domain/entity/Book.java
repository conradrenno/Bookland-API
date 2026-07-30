package com.devrenno.bookland.catalog.domain.entity;

import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;
import com.devrenno.bookland.catalog.domain.valueobject.BookId;
import com.devrenno.bookland.catalog.domain.valueobject.CategoryId;
import com.devrenno.bookland.catalog.domain.valueobject.ISBN;
import com.devrenno.bookland.catalog.domain.valueobject.Price;
import lombok.Getter;

import java.time.Instant;
import java.util.List;

@Getter
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
    private String coverImageUrl;
    private double avgRating;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Book(BookId id, String title, ISBN isbn, List<String> authors, String publisher,
                 Integer publicationYear, String edition, String synopsis, Price price, int stockQuantity,
                 CategoryId categoryId, String coverImageUrl, double avgRating, boolean active,
                 Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.title = title;
        this.isbn = isbn;
        this.authors = authors;
        this.publisher = publisher;
        this.publicationYear = publicationYear;
        this.edition = edition;
        this.synopsis = synopsis;
        this.price = price;
        this.stockQuantity = stockQuantity;
        this.categoryId = categoryId;
        this.coverImageUrl = coverImageUrl;
        this.avgRating = avgRating;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Book create(String title, ISBN isbn, List<String> authors, String publisher,
                              Integer publicationYear, String edition, String synopsis, Price price,
                              int stockQuantity, CategoryId categoryId, String coverImageUrl) {
        Instant now = Instant.now();
        return new Book(BookId.generate(), title, isbn, authors, publisher, publicationYear, edition,
                synopsis, price, stockQuantity, categoryId, coverImageUrl, 0.0, true, now, now);
    }

    public static Book reconstitute(BookId id, String title, ISBN isbn, List<String> authors, String publisher,
                                    Integer publicationYear, String edition, String synopsis, Price price,
                                    int stockQuantity, CategoryId categoryId, String coverImageUrl,
                                    double avgRating, boolean active,
                                    Instant createdAt, Instant updatedAt) {
        return new Book(id, title, isbn, authors, publisher, publicationYear, edition, synopsis, price,
                stockQuantity, categoryId, coverImageUrl, avgRating, active, createdAt, updatedAt);
    }

    public void update(String title, List<String> authors, String publisher, Integer publicationYear,
                       String edition, String synopsis, Price price, int stockQuantity, CategoryId categoryId,
                       String coverImageUrl) {
        if (title != null) this.title = title;
        if (authors != null) this.authors = authors;
        if (publisher != null) this.publisher = publisher;
        if (publicationYear != null) this.publicationYear = publicationYear;
        if (edition != null) this.edition = edition;
        if (synopsis != null) this.synopsis = synopsis;
        if (price != null) this.price = price;
        if (stockQuantity >= 0) this.stockQuantity = stockQuantity;
        if (categoryId != null) this.categoryId = categoryId;
        if (coverImageUrl != null) this.coverImageUrl = coverImageUrl;
        this.updatedAt = Instant.now();
    }

    public void updateCoverImage(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
        this.updatedAt = Instant.now();
    }

    public void adjustStock(int delta) {
        int newQty = this.stockQuantity + delta;
        if (newQty < 0) {
            throw new InsufficientStockException(this.id.value(), this.stockQuantity, delta);
        }
        this.stockQuantity = newQty;
        this.updatedAt = Instant.now();
    }

    public void updateAverageRating(double newAvgRating) {
        this.avgRating = newAvgRating;
        this.updatedAt = Instant.now();
    }

    public void deactivate() {
        this.active = false;
        this.updatedAt = Instant.now();
    }
}
