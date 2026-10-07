package com.devrenno.bookland.catalog.infrastructure.grpc;

import com.devrenno.bookland.catalog.application.port.in.GetBooksByIdsUseCase;
import com.devrenno.bookland.catalog.domain.entity.Book;
import com.devrenno.bookland.catalog.infrastructure.grpc.v1.BookCatalogGrpc;
import com.devrenno.bookland.catalog.infrastructure.grpc.v1.GetBooksRequest;
import com.devrenno.bookland.catalog.infrastructure.grpc.v1.GetBooksResponse;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * The catalog's gRPC endpoint: the messaging-free twin of {@code BookApiController}, turning a call
 * into a use-case call and the books into the contract's messages. Spring gRPC serves every
 * {@code BindableService} bean, which the generated base class is.
 *
 * <p>Reads only active books, through the same rule the public HTTP lookup applies, so a book removed
 * from the catalog cannot be added to a cart over gRPC either.
 */
@Component
public class BookCatalogGrpcService extends BookCatalogGrpc.BookCatalogImplBase {

    private final GetBooksByIdsUseCase getBooksByIdsUseCase;

    public BookCatalogGrpcService(GetBooksByIdsUseCase getBooksByIdsUseCase) {
        this.getBooksByIdsUseCase = getBooksByIdsUseCase;
    }

    @Override
    public void getBooks(GetBooksRequest request, StreamObserver<GetBooksResponse> responseObserver) {
        List<UUID> ids;
        try {
            ids = request.getBookIdsList().stream().map(UUID::fromString).toList();
        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT.withDescription(e.getMessage()).asRuntimeException());
            return;
        }
        GetBooksResponse.Builder response = GetBooksResponse.newBuilder();
        for (Book book : getBooksByIdsUseCase.execute(ids)) {
            response.addBooks(toMessage(book));
        }
        responseObserver.onNext(response.build());
        responseObserver.onCompleted();
    }

    private static com.devrenno.bookland.catalog.infrastructure.grpc.v1.Book toMessage(Book book) {
        return com.devrenno.bookland.catalog.infrastructure.grpc.v1.Book.newBuilder()
                .setId(book.getId().value().toString())
                .setTitle(book.getTitle())
                .setCoverImageUrl(book.getCoverImageUrl() == null ? "" : book.getCoverImageUrl())
                .setPrice(book.getPrice().value().toPlainString())
                .setStockQuantity(book.getStockQuantity())
                .build();
    }
}
