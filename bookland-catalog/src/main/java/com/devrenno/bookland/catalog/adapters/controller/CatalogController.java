package com.devrenno.bookland.catalog.adapters.controller;

import com.devrenno.bookland.catalog.adapters.presenter.CatalogPresenter;
import com.devrenno.bookland.catalog.adapters.viewmodel.BookViewModel;
import com.devrenno.bookland.catalog.adapters.viewmodel.CategoryViewModel;
import com.devrenno.bookland.catalog.application.common.PageQuery;
import com.devrenno.bookland.catalog.application.common.PageResult;
import com.devrenno.bookland.catalog.application.dto.BookSearchQuery;
import com.devrenno.bookland.catalog.application.dto.CreateBookCommand;
import com.devrenno.bookland.catalog.application.dto.UpdateBookCommand;
import com.devrenno.bookland.catalog.application.port.in.*;
import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import com.devrenno.bookland.catalog.application.port.out.BookPersistencePort;
import com.devrenno.bookland.catalog.application.port.out.CategoryPersistencePort;
import com.devrenno.bookland.catalog.application.service.*;
import com.devrenno.bookland.catalog.domain.service.CatalogDomainService;

import java.util.List;
import java.util.UUID;

/**
 * Internal controller: orchestrates the catalog HTTP-facing use cases and delegates to the Presenter.
 * Also the module's composition root for HTTP delivery. Framework-free.
 * (Cross-module use cases — GetBookById, CreateBook, stock/rating/low-stock — are exposed as beans.)
 */
public class CatalogController {

    private final CreateBookUseCase createBookUseCase;
    private final GetBookByIdUseCase getBookByIdUseCase;
    private final SearchBooksUseCase searchBooksUseCase;
    private final UpdateBookUseCase updateBookUseCase;
    private final RemoveBookUseCase removeBookUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final ListBooksByCategoryUseCase listBooksByCategoryUseCase;
    private final CatalogPresenter presenter;

    private CatalogController(CreateBookUseCase createBookUseCase, GetBookByIdUseCase getBookByIdUseCase,
                            SearchBooksUseCase searchBooksUseCase, UpdateBookUseCase updateBookUseCase,
                            RemoveBookUseCase removeBookUseCase, ListCategoriesUseCase listCategoriesUseCase,
                            ListBooksByCategoryUseCase listBooksByCategoryUseCase, CatalogPresenter presenter) {
        this.createBookUseCase = createBookUseCase;
        this.getBookByIdUseCase = getBookByIdUseCase;
        this.searchBooksUseCase = searchBooksUseCase;
        this.updateBookUseCase = updateBookUseCase;
        this.removeBookUseCase = removeBookUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.listBooksByCategoryUseCase = listBooksByCategoryUseCase;
        this.presenter = presenter;
    }

    public static CatalogController create(BookPersistencePort bookPersistencePort,
                                           CategoryPersistencePort categoryPersistencePort,
                                           ActiveOrderCheckPort activeOrderCheckPort) {
        CatalogDomainService domainService = new CatalogDomainService();
        return new CatalogController(
                CreateBookService.create(domainService, bookPersistencePort, categoryPersistencePort),
                GetBookByIdService.create(bookPersistencePort),
                SearchBooksService.create(bookPersistencePort),
                UpdateBookService.create(bookPersistencePort, categoryPersistencePort),
                RemoveBookService.create(bookPersistencePort, activeOrderCheckPort),
                ListCategoriesService.create(categoryPersistencePort),
                ListBooksByCategoryService.create(bookPersistencePort, categoryPersistencePort),
                CatalogPresenter.create()
        );
    }

    public BookViewModel createBook(CreateBookCommand command) {
        return presenter.present(createBookUseCase.execute(command));
    }

    public BookViewModel getBook(UUID bookId) {
        return presenter.present(getBookByIdUseCase.execute(bookId));
    }

    public PageResult<BookViewModel> search(BookSearchQuery query) {
        return searchBooksUseCase.execute(query).map(presenter::present);
    }

    public BookViewModel updateBook(UUID bookId, UpdateBookCommand command) {
        return presenter.present(updateBookUseCase.execute(bookId, command));
    }

    public void removeBook(UUID bookId) {
        removeBookUseCase.execute(bookId);
    }

    public List<CategoryViewModel> listCategories() {
        return listCategoriesUseCase.execute().stream().map(presenter::present).toList();
    }

    public PageResult<BookViewModel> listBooksByCategory(UUID categoryId, PageQuery pageQuery) {
        return listBooksByCategoryUseCase.execute(categoryId, pageQuery).map(presenter::present);
    }
}
