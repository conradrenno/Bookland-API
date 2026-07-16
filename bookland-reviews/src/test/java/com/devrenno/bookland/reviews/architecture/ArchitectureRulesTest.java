package com.devrenno.bookland.reviews.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the 4-layer Clean Architecture of the reviews module. Domain, Application and Adapters
 * must stay framework-free (Lombok is source-only; depending on the framework-free catalog/orders
 * ports and exceptions is allowed). Only Infrastructure may touch Spring / JPA / Jackson.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland.reviews", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final String[] FRAMEWORK_PACKAGES = {
            "org.springframework..",
            "jakarta.persistence..",
            "com.fasterxml.jackson.."
    };

    @ArchTest
    static final ArchRule domain_is_framework_free =
            noClasses().that().resideInAPackage("..reviews.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule application_is_framework_free =
            noClasses().that().resideInAPackage("..reviews.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule adapters_are_framework_free =
            noClasses().that().resideInAPackage("..reviews.adapters..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..reviews.domain..")
            .layer("Application").definedBy("..reviews.application..")
            .layer("Adapters").definedBy("..reviews.adapters..")
            .layer("Infrastructure").definedBy("..reviews.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Infrastructure");
}
