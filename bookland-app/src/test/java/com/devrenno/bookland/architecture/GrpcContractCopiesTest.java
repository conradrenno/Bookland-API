package com.devrenno.bookland.architecture;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Each gRPC contract has one owner, and every consumer keeps a copy of the owner's .proto to generate
 * its own stubs (decision of step 5: no shared contract module, as no shared messaging library). A
 * copy that drifts compiles and runs — until a field number or a type disagrees on the wire, where it
 * fails as garbage, not as an error. This test is what notices first.
 *
 * <p>Compared after dropping comments, {@code java_package} (the one line a copy must change, so the
 * generated classes do not collide on the monolith's classpath) and whitespace. Everything that reaches
 * the wire — package, service, methods, messages, field numbers and types — must match.
 *
 * <p>Reads the files from the source tree: Maven runs the tests with the module directory as the
 * working directory, so the other modules are one level up.
 */
class GrpcContractCopiesTest {

    @ParameterizedTest(name = "{1} copy of {0}")
    @CsvSource({
            "bookland-catalog, bookland-orders,   bookland/catalog/v1/book_catalog.proto",
            "bookland-catalog, bookland-wishlist, bookland/catalog/v1/book_catalog.proto",
            "bookland-catalog, bookland-reviews,  bookland/catalog/v1/book_catalog.proto",
            "bookland-orders,  bookland-catalog,  bookland/orders/v1/order_activity.proto",
    })
    void copyMatchesTheOwner(String owner, String consumer, String file) throws IOException {
        assertThat(wireContract(consumer, file))
                .as("%s's copy of %s, against %s's", consumer, file, owner)
                .isEqualTo(wireContract(owner, file));
    }

    private static String wireContract(String module, String file) throws IOException {
        String proto = Files.readString(Path.of("..", module, "src", "main", "protobuf", file));
        return proto
                .replaceAll("(?m)//.*$", "")
                .replaceAll("option\\s+java_package\\s*=\\s*\"[^\"]*\"\\s*;", "")
                .replaceAll("\\s+", " ")
                .trim();
    }
}
