package com.devrenno.bookland.catalog.domain.valueobject;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ISBNTest {

    @ParameterizedTest
    @ValueSource(strings = {"9780132350884", "978-0132350884", "978-0-13-235088-4", " 9780132350884 "})
    void of_shouldNormalizeToThirteenDigits(String input) {
        assertThat(ISBN.of(input).value()).isEqualTo("9780132350884");
    }

    @Test
    void equals_shouldIgnoreHyphens() {
        assertThat(ISBN.of("978-0132350884")).isEqualTo(ISBN.of("9780132350884"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"978013235088", "97801323508845", "978-013235088X", "abc"})
    void of_shouldRejectAnythingThatIsNotThirteenDigits(String input) {
        assertThatThrownBy(() -> ISBN.of(input)).isInstanceOf(IllegalArgumentException.class);
    }
}
