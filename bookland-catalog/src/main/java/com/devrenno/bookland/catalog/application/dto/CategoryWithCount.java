package com.devrenno.bookland.catalog.application.dto;

import java.util.UUID;

/**
 * Query read-model: an active category with its (aggregate) book count.
 */
public record CategoryWithCount(UUID id, String name, long bookCount) {}
