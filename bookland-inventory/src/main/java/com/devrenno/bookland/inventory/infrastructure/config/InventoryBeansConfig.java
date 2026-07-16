package com.devrenno.bookland.inventory.infrastructure.config;

import com.devrenno.bookland.inventory.adapters.controller.InventoryController;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.application.port.out.LowStockBooksPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Composition root of the inventory module. Builds the framework-free inner graph from the outbound
 * ports (implemented by Spring adapters) and exposes the internal InventoryController as a bean.
 */
@Configuration
public class InventoryBeansConfig {

    @Bean
    public InventoryController inventoryController(BookStockAdjustmentPort bookStockAdjustmentPort,
                                                   InventoryPersistencePort inventoryPersistencePort,
                                                   LowStockBooksPort lowStockBooksPort) {
        return InventoryController.create(bookStockAdjustmentPort, inventoryPersistencePort, lowStockBooksPort);
    }
}
