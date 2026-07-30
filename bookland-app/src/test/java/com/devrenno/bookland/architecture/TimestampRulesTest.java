package com.devrenno.bookland.architecture;

import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import java.time.LocalDateTime;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.fields;

/**
 * A point in time must be stored as one.
 *
 * <p>{@code LocalDateTime} names a wall clock without saying whose, so it does not identify an
 * instant. Serialized straight to JSON it produced {@code "2026-07-30T13:01:34.4862"}, which every
 * consumer resolved in whatever zone its own process happened to run in — the same row read as
 * 13:01 on a machine in America/Sao_Paulo and 10:01 in a UTC container. Under SSR that is worse
 * than a wrong time: the server renders one value and the browser re-renders another, so the text
 * changes by itself after the page loads.
 *
 * <p>No Jackson setting can fix this, because the missing zone is missing from the <em>type</em>.
 * The fix is {@code Instant} everywhere, which serializes with a {@code Z}. This rule keeps it
 * that way — the inner layers are framework-free, so nothing else would catch a reintroduction.
 *
 * <p>Lives in bookland-app because it has every module on the classpath at once, like
 * {@link WebLayerRulesTest}.
 */
@AnalyzeClasses(packages = "com.devrenno.bookland", importOptions = ImportOption.DoNotIncludeTests.class)
class TimestampRulesTest {

    @ArchTest
    static final ArchRule instants_are_stored_as_instants = fields()
            .should().notHaveRawType(LocalDateTime.class)
            .because("LocalDateTime carries no zone, so it does not identify an instant: serialized "
                    + "to JSON it is read in the zone of whichever process parses it, and the same "
                    + "value shifts by hours between a developer machine and a UTC container. Use "
                    + "Instant, which renders with a Z");
}
