package com.devrenno.bookland.wishlist.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the 4-layer Clean Architecture of the wishlist module. Domain, Application and Adapters
 * must stay framework-free (Lombok is source-only). Only Infrastructure may touch Spring / JPA / Jackson.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland.wishlist", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final String[] FRAMEWORK_PACKAGES = {
            "org.springframework..",
            "jakarta.persistence..",
            "com.fasterxml.jackson.."
    };

    @ArchTest
    static final ArchRule domain_is_framework_free =
            noClasses().that().resideInAPackage("..wishlist.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule application_is_framework_free =
            noClasses().that().resideInAPackage("..wishlist.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule adapters_are_framework_free =
            noClasses().that().resideInAPackage("..wishlist.adapters..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..wishlist.domain..")
            .layer("Application").definedBy("..wishlist.application..")
            .layer("Adapters").definedBy("..wishlist.adapters..")
            .layer("Infrastructure").definedBy("..wishlist.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Infrastructure");
}
