package com.devrenno.bookland.catalog.adapters.viewmodel;

import java.util.UUID;

public record CategoryViewModel(UUID id, String name, long bookCount) {}
