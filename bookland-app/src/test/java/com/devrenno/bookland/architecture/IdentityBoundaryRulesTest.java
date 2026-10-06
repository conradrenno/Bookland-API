package com.devrenno.bookland.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The identity service (user + auth) is about to run in a process of its own, so nothing else may
 * reach into it: what crosses the boundary is the access token, read through bookland-web-support.
 *
 * <p>The two modules may depend on each other — they leave together. Everything else, the assembly
 * module included, must not touch either. A violation here is a class that would stop compiling the
 * day the identity service is extracted, caught while it is still cheap to fix.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland", importOptions = ImportOption.DoNotIncludeTests.class)
class IdentityBoundaryRulesTest {

    private static final String[] IDENTITY = {"com.devrenno.bookland.user..", "com.devrenno.bookland.auth.."};

    @ArchTest
    static final ArchRule nothing_outside_identity_depends_on_it = noClasses()
            .that().resideOutsideOfPackages(IDENTITY)
            .should().dependOnClassesThat().resideInAnyPackage(IDENTITY)
            .because("user and auth become the identity service; other modules learn who the caller "
                    + "is from the access token (AuthenticatedUser), never from the user module");
}
