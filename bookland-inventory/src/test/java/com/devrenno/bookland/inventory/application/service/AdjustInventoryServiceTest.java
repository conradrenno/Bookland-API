package com.devrenno.bookland.inventory.application.service;

import com.devrenno.bookland.catalog.domain.exception.BookNotFoundException;
import com.devrenno.bookland.catalog.domain.exception.InsufficientStockException;
import com.devrenno.bookland.inventory.application.dto.AdjustInventoryCommand;
import com.devrenno.bookland.inventory.application.port.out.BookStockAdjustmentPort;
import com.devrenno.bookland.inventory.application.port.out.InventoryPersistencePort;
import com.devrenno.bookland.inventory.application.port.out.TransactionPort;
import com.devrenno.bookland.inventory.domain.entity.InventoryEntry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdjustInventoryServiceTest {

    @Mock private BookStockAdjustmentPort bookStockAdjustmentPort;
    @Mock private InventoryPersistencePort inventoryPersistencePort;

    /** Pass-through fake: runs the unit of work inline, no transaction machinery in unit tests. */
    private final TransactionPort transactionPort = new TransactionPort() {
        @Override
        public void inTransaction(Runnable work) {
            work.run();
        }

        @Override
        public <T> T inTransaction(Supplier<T> work) {
            return work.get();
        }
    };

    private AdjustInventoryService service;

    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = AdjustInventoryService.create(bookStockAdjustmentPort, inventoryPersistencePort,
                transactionPort);
    }

    @Test
    void execute_shouldRecordEntryAndReturnIt_whenDeltaIsValid() {
        UUID adminId = UUID.randomUUID();
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, 10, "Restock", adminId);

        when(bookStockAdjustmentPort.adjustStock(bookId, 10)).thenReturn(15);
        when(inventoryPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryEntry result = service.execute(command);

        assertThat(result.getPreviousQuantity()).isEqualTo(5);
        assertThat(result.getNewQuantity()).isEqualTo(15);
        assertThat(result.getDelta()).isEqualTo(10);
        verify(inventoryPersistencePort).save(any(InventoryEntry.class));
    }

    @Test
    void execute_shouldRecordEntry_whenDeltaIsNegative() {
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, -4, "Damaged", null);

        when(bookStockAdjustmentPort.adjustStock(bookId, -4)).thenReturn(6);
        when(inventoryPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryEntry result = service.execute(command);

        assertThat(result.getPreviousQuantity()).isEqualTo(10);
        assertThat(result.getNewQuantity()).isEqualTo(6);
        assertThat(result.getDelta()).isEqualTo(-4);
    }

    /**
     * The ledger's previous quantity is derived from what the adjustment produced, never read
     * beforehand — a separate read is a different snapshot, and recording it would make the audit
     * trail disagree with the stock it is supposed to explain.
     */
    @Test
    void execute_shouldNotReadStockBeforeAdjusting() {
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, 3, null, null);

        when(bookStockAdjustmentPort.adjustStock(bookId, 3)).thenReturn(8);
        when(inventoryPersistencePort.save(any())).thenAnswer(inv -> inv.getArgument(0));

        InventoryEntry result = service.execute(command);

        assertThat(result.getPreviousQuantity()).isEqualTo(5);
        verifyNoMoreInteractions(bookStockAdjustmentPort);
    }

    @Test
    void execute_shouldPropagateInsufficientStockException_whenDeltaMakesStockNegative() {
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, -100, null, null);

        when(bookStockAdjustmentPort.adjustStock(bookId, -100))
                .thenThrow(new InsufficientStockException(bookId, 5, -100));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(InsufficientStockException.class);

        verify(inventoryPersistencePort, never()).save(any());
    }

    @Test
    void execute_shouldPropagateBookNotFoundException_whenBookDoesNotExist() {
        AdjustInventoryCommand command = new AdjustInventoryCommand(bookId, 5, null, null);

        when(bookStockAdjustmentPort.adjustStock(bookId, 5))
                .thenThrow(new BookNotFoundException(bookId));

        assertThatThrownBy(() -> service.execute(command))
                .isInstanceOf(BookNotFoundException.class);

        verify(inventoryPersistencePort, never()).save(any());
    }
}
