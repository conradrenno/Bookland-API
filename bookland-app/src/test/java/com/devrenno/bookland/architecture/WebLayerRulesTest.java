package com.devrenno.bookland.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaMethod;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.Set;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.methods;

/**
 * Rules over the web layer of every module at once.
 *
 * <p>Lives in bookland-app rather than being copied into each module's {@code ArchitectureRulesTest}
 * because those analyse one module's package each; this one needs the whole application on the
 * classpath, which is exactly what the assembly module has.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland", importOptions = ImportOption.DoNotIncludeTests.class)
class WebLayerRulesTest {

    /**
     * {@code ResponseEntity} factories that produce something other than 200. {@code ok(..)} is
     * absent on purpose — it is the status springdoc already assumes.
     */
    private static final Set<String> NON_DEFAULT_STATUS_FACTORIES =
            Set.of("status", "created", "noContent", "accepted");

    @ArchTest
    static final ArchRule non_default_success_status_is_declared = methods()
            .that().areDeclaredInClassesThat().resideInAPackage("..infrastructure.web..")
            .and(buildAResponseWithANonDefaultStatus())
            .should().beAnnotatedWith(ResponseStatus.class)
            .because("springdoc infers the success code from the return type and cannot see through "
                    + "ResponseEntity.status(...); without @ResponseStatus the published OpenAPI "
                    + "document claims 200 and every generated client gets the wrong success type "
                    + "(see docs/error-contract.md)")
            // Modules whose handlers all answer 200 legitimately match nothing here.
            .allowEmptyShould(true);

    private static DescribedPredicate<JavaMethod> buildAResponseWithANonDefaultStatus() {
        return new DescribedPredicate<>("build a ResponseEntity with a status other than 200") {
            @Override
            public boolean test(JavaMethod method) {
                return method.getMethodCallsFromSelf().stream()
                        .anyMatch(call -> call.getTargetOwner().isEquivalentTo(ResponseEntity.class)
                                && NON_DEFAULT_STATUS_FACTORIES.contains(call.getName()));
            }
        };
    }
}
