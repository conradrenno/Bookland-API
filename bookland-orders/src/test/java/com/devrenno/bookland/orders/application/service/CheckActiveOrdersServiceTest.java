package com.devrenno.bookland.orders.application.service;

import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CheckActiveOrdersServiceTest {

    @Mock private OrderPersistencePort orderPersistencePort;

    private CheckActiveOrdersService service;

    private final UUID bookId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = CheckActiveOrdersService.create(orderPersistencePort);
    }

    @Test
    void hasActiveOrdersForBook_shouldQueryOnlyInProgressStatuses() {
        Set<OrderStatus> expectedActive = Set.of(
                OrderStatus.AWAITING_PAYMENT, OrderStatus.CONFIRMED, OrderStatus.SHIPPED);

        when(orderPersistencePort.existsOrderWithBookInStatuses(bookId, expectedActive)).thenReturn(true);

        assertThat(service.hasActiveOrdersForBook(bookId)).isTrue();
    }

    @Test
    void hasActiveOrdersForBook_shouldReturnFalse_whenNoActiveOrderContainsBook() {
        when(orderPersistencePort.existsOrderWithBookInStatuses(eq(bookId), eq(OrderStatus.activeStatuses())))
                .thenReturn(false);

        assertThat(service.hasActiveOrdersForBook(bookId)).isFalse();
    }
}
