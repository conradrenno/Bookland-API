package com.devrenno.bookland.catalog.application.common;

/**
 * Framework-free sort options for book search. The persistence adapter translates these into the
 * concrete (Spring) Sort.
 */
public enum BookSort {
    TITLE,
    PRICE_ASC,
    PRICE_DESC,
    RATING_DESC
}
