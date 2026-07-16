package com.devrenno.bookland.orders.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import com.devrenno.bookland.orders.application.port.in.CheckActiveOrdersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Implements the CATALOG module's outbound port. Lives here (not in catalog) because the Maven
 * dependency points orders -> catalog: catalog defines the port it needs, orders fulfills it —
 * classic dependency inversion across modules.
 */
@Component
@RequiredArgsConstructor
public class ActiveOrderCheckAdapter implements ActiveOrderCheckPort {

    private final CheckActiveOrdersUseCase checkActiveOrdersUseCase;

    @Override
    public boolean hasActiveOrdersForBook(UUID bookId) {
        return checkActiveOrdersUseCase.hasActiveOrdersForBook(bookId);
    }
}
