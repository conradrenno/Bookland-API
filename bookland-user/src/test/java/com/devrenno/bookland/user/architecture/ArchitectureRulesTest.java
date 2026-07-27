package com.devrenno.bookland.user.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the 4-layer Clean Architecture of the module.
 * <p>
 * Domain, Application and Adapters must stay framework-free (Lombok is source-only,
 * so it leaves no trace in bytecode and is allowed). Only Infrastructure may touch
 * Spring / JPA / Jackson. Dependencies must always point inward.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland.user", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureRulesTest {

    private static final String[] FRAMEWORK_PACKAGES = {
            "org.springframework..",
            "jakarta.persistence..",
            "com.fasterxml.jackson..",
            // Boot 4 ships Jackson 3, whose package is tools.jackson. Listing only the old
            // coordinates leaves this rule looking right while enforcing nothing.
            "tools.jackson..",
            // bookland-web-support is HTTP infrastructure (problem+json, security entry
            // points, the validation advice) and must not leak inward either.
            "com.devrenno.bookland.websupport.."
    };

    @ArchTest
    static final ArchRule domain_is_framework_free =
            noClasses().that().resideInAPackage("..user.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule application_is_framework_free =
            noClasses().that().resideInAPackage("..user.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule adapters_are_framework_free =
            noClasses().that().resideInAPackage("..user.adapters..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            .layer("Domain").definedBy("..user.domain..")
            .layer("Application").definedBy("..user.application..")
            .layer("Adapters").definedBy("..user.adapters..")
            .layer("Infrastructure").definedBy("..user.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Infrastructure");
}
