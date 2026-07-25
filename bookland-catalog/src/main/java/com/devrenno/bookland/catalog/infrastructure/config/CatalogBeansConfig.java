package com.devrenno.bookland.catalog.infrastructure.config;

import com.devrenno.bookland.catalog.adapters.controller.CatalogController;
import com.devrenno.bookland.catalog.application.port.in.*;
import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.ImageStoragePort;
import com.devrenno.bookland.catalog.application.service.*;
import com.devrenno.bookland.catalog.domain.service.CatalogDomainService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the catalog module. Exposes the internal CatalogController (HTTP delivery)
 * plus the cross-module boundary use cases consumed by other modules:
 * GetBookById (orders/reviews/wishlist), CreateBook (bookland-app seed), stock/rating/low-stock
 * (inventory, orders, reviews).
 */
@Configuration
public class CatalogBeansConfig {

    @Bean
    public CatalogController catalogController(BookPersistencePort bookPersistencePort,
                                               CategoryPersistencePort categoryPersistencePort,
                                               ActiveOrderCheckPort activeOrderCheckPort,
                                               ImageStoragePort imageStoragePort) {
        return CatalogController.create(bookPersistencePort, categoryPersistencePort,
                activeOrderCheckPort, imageStoragePort);
    }

    @Bean
    public GetBookByIdUseCase getBookByIdUseCase(BookPersistencePort bookPersistencePort) {
        return GetBookByIdService.create(bookPersistencePort);
    }

    @Bean
    public CreateBookUseCase createBookUseCase(BookPersistencePort bookPersistencePort,
                                               CategoryPersistencePort categoryPersistencePort) {
        return CreateBookService.create(new CatalogDomainService(), bookPersistencePort, categoryPersistencePort);
    }

    @Bean
    public GetBookStockUseCase getBookStockUseCase(BookPersistencePort bookPersistencePort) {
        return GetBookStockService.create(bookPersistencePort);
    }

    @Bean
    public AdjustBookStockUseCase adjustBookStockUseCase(BookPersistencePort bookPersistencePort) {
        return AdjustBookStockService.create(bookPersistencePort);
    }

    @Bean
    public GetLowStockBooksUseCase getLowStockBooksUseCase(BookPersistencePort bookPersistencePort) {
        return GetLowStockBooksService.create(bookPersistencePort);
    }

    @Bean
    public UpdateBookAverageRatingUseCase updateBookAverageRatingUseCase(BookPersistencePort bookPersistencePort) {
        return UpdateBookAverageRatingService.create(bookPersistencePort);
    }
}
