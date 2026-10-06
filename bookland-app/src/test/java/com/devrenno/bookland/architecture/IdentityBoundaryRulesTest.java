package com.devrenno.bookland.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

/**
 * The identity service (user + auth) runs in a process of its own, so nothing here may reach into
 * it: what crosses the boundary is the access token, read through bookland-web-support.
 *
 * <p>Since the extraction neither module is on this application's classpath, so the compiler already
 * refuses a direct use. The rule stays for the day someone adds the dependency back to make a quick
 * call compile: it fails the build with the reason, instead of letting the monolith quietly grow a
 * second copy of the identity service.
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
