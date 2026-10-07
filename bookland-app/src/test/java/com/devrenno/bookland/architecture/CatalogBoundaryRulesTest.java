package com.devrenno.bookland.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * catalog and inventory become the catalog service (step 5). Everyone else reaches them over gRPC,
 * through stubs generated from their own copy of the catalog's .proto, and over Kafka — never by
 * importing a catalog class. Lives here because only this module has every module on one classpath.
 *
 * <p>The stubs a consumer generates sit in its own package ({@code orders.infrastructure.grpc.catalog.v1}),
 * so they do not count as the catalog's classes, which is the point.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland", importOptions = ImportOption.DoNotIncludeTests.class)
class CatalogBoundaryRulesTest {

    private static final String[] CATALOG = {"com.devrenno.bookland.catalog..", "com.devrenno.bookland.inventory.."};

    @ArchTest
    static final ArchRule nothing_outside_the_catalog_depends_on_it = noClasses()
            .that().resideOutsideOfPackages(CATALOG)
            .should().dependOnClassesThat().resideInAnyPackage(CATALOG)
            .because("catalog and inventory become the catalog service; other modules read books over "
                    + "gRPC (BookCatalog) and react to stock over Kafka, never through a catalog class");
}
