package com.devrenno.bookland.catalog.adapters.presenter;

import com.devrenno.bookland.catalog.adapters.viewmodel.BookViewModel;
import com.devrenno.bookland.catalog.adapters.viewmodel.CategoryViewModel;
import com.devrenno.bookland.catalog.application.dto.CategoryWithCount;
import com.devrenno.bookland.catalog.domain.entity.Book;

/**
 * Transforms domain books / category read-models into delivery-facing view models. Plain Java.
 */
public class CatalogPresenter {

    private CatalogPresenter() {
    }

    public static CatalogPresenter create() {
        return new CatalogPresenter();
    }

    public BookViewModel present(Book book) {
        return new BookViewModel(
                book.getId().value(),
                book.getTitle(),
                book.getIsbn().value(),
                book.getAuthors(),
                book.getPublisher(),
                book.getPublicationYear(),
                book.getEdition(),
                book.getSynopsis(),
                book.getPrice().value(),
                book.getStockQuantity(),
                book.isActive() && book.getStockQuantity() > 0,
                book.getCategoryId().value(),
                book.getCoverImageUrl(),
                book.getAvgRating()
        );
    }

    public CategoryViewModel present(CategoryWithCount category) {
        return new CategoryViewModel(category.id(), category.name(), category.bookCount());
    }
}
