package com.devrenno.bookland.catalog.infrastructure.adapter;

import com.devrenno.bookland.catalog.application.port.out.ActiveOrderCheckPort;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class ActiveOrderCheckAdapter implements ActiveOrderCheckPort {

    @Override
    public boolean hasActiveOrdersForBook(UUID bookId) {
        // Stub: always returns false until the orders domain is implemented
        return false;
    }
}
