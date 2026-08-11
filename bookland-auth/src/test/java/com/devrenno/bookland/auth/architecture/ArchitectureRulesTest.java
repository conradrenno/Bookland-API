package com.devrenno.bookland.auth.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;

/**
 * Enforces the 4-layer Clean Architecture of the auth module. Domain, Application and Adapters
 * must stay framework-free (Lombok is source-only; depending on the framework-free bookland-user
 * domain/ports is allowed). Only Infrastructure may touch Spring / JPA / Jackson. Dependencies
 * must always point inward.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland.auth", importOptions = ImportOption.DoNotIncludeTests.class)
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

    /**
     * The auth module currently has <strong>no domain layer</strong>, and that is a finding rather
     * than an accident: every class that used to live there — {@code RefreshToken}, {@code Token},
     * {@code AuthTokens} and the four token exceptions — existed to support a hand-rolled
     * implementation of what the Authorization Server now does. What is left of the module is an
     * adapter between Spring Security and {@code bookland-user}, and the business rules of
     * registering (e-mail uniqueness, the default role, the invariants of {@code User.create}) were
     * always in the user module, never here.
     *
     * <p>The rule is kept, empty, instead of deleted: it costs nothing and it is the guard that
     * fires the day someone puts a domain class back. {@code allowEmptyShould} is what lets a rule
     * that matches nothing pass — without it ArchUnit fails on the assumption that a rule checking
     * zero classes is a typo, which is usually right.
     */
    @ArchTest
    static final ArchRule domain_is_framework_free =
            noClasses().that().resideInAPackage("..auth.domain..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES)
                    .allowEmptyShould(true);

    @ArchTest
    static final ArchRule application_is_framework_free =
            noClasses().that().resideInAPackage("..auth.application..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule adapters_are_framework_free =
            noClasses().that().resideInAPackage("..auth.adapters..")
                    .should().dependOnClassesThat().resideInAnyPackage(FRAMEWORK_PACKAGES);

    @ArchTest
    static final ArchRule dependencies_point_inward = layeredArchitecture()
            .consideringOnlyDependenciesInLayers()
            // optionalLayer, not layer: the domain package is empty today (see above). A plain
            // layer() would fail the build for being empty, which would say "your architecture is
            // broken" about a module that simply has no business rules of its own.
            .optionalLayer("Domain").definedBy("..auth.domain..")
            .layer("Application").definedBy("..auth.application..")
            .layer("Adapters").definedBy("..auth.adapters..")
            .layer("Infrastructure").definedBy("..auth.infrastructure..")
            .whereLayer("Infrastructure").mayNotBeAccessedByAnyLayer()
            .whereLayer("Adapters").mayOnlyBeAccessedByLayers("Infrastructure")
            .whereLayer("Application").mayOnlyBeAccessedByLayers("Adapters", "Infrastructure")
            .whereLayer("Domain").mayOnlyBeAccessedByLayers("Application", "Adapters", "Infrastructure");
}
