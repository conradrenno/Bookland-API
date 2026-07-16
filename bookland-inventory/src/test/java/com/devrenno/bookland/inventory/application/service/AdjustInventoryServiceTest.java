package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;
import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustInventoryServiceTest {

    @Mock private BookStockAdjustmentPort bookStockAdjustmentPort;
    @Mock private InventoryPersistencePort inventoryPersistencePort;

    private AdjustInventoryService service;

    @BeforeEach
    void setUp() {
        service = AdjustInventoryService.create(bookStockAdjustmentPort, inventoryPersistencePort);
    }

    @Test
    void execute_shouldRecordEntryAndReturnIt_whenDeltaIsValid() {
        UUID bookId = UUID.randomUUID();
        UUID adminId = UUID.randomUUID();
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, 10, "Restock", adminId);

        when(bookStockAdjustmentPort.getCurrentStock(bookId)).thenReturn(5);
        when(bookStockAdjustmentPort.adjustStock(bookId, 10)).thenReturn(15);
        when(inventoryPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryEntry result = service.execute(command);

        assertThat(result.getPreviousQuantity()).isEqualTo(5);
        assertThat(result.getNewQuantity()).isEqualTo(15);
        assertThat(result.getDelta()).isEqualTo(10);
        verify(inventoryPersistencePort).save(any(InventoryEntry.class));
    }

    @Test
    void execute_shouldPropagateInsufficientStockException_whenDeltaMakesStockNegative() {
        UUID bookId = UUID.randomUUID();
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, -100, null, null);

        when(bookStockAdjustmentPort.getCurrentStock(bookId)).thenReturn(5);
        when(bookStockAdjustmentPort.adjustStock(bookId, -100))
                .thenThrow(new InsufficientStockException(bookId, 5, -100));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldPropagateBookNotFoundException_whenBookDoesNotExist() {
        UUID bookId = UUID.randomUUID();
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, 5, null, null);

        when(bookStockAdjustmentPort.getCurrentStock(bookId))
                .thenThrow(new BookNotFoundException(bookId));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BookNotFoundException.class);

        verify(inventoryPersistencePort, never()).save(any());
    }
}
