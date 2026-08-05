package com.devrenno.bookland;

import com.devrenno.bookland.orders.application.common.PageQuery;
import com.devrenno.bookland.orders.application.common.PageResult;
import com.devrenno.bookland.orders.application.port.out.OrderPersistencePort;
import com.devrenno.bookland.orders.domain.entity.Order;
import com.devrenno.bookland.orders.domain.entity.OrderItem;
import com.devrenno.bookland.orders.domain.entity.OrderStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Every listing of orders answers newest first, and pages without losing a row.
 *
 * <p>This runs against the real query and the real database rather than a mocked repository,
 * because the defect it pins lived in neither the use case nor the entity: the persistence
 * adapter built a {@code PageRequest} with no {@code Sort}, and an unsorted query returns rows
 * in whatever order the database finds convenient — which happened to be oldest first, putting
 * the customer's newest order on the last page of their own history.
 */
@SpringBootTest
@ActiveProfiles("dev")
class OrderHistoryOrderingIntegrationTest {

    @Autowired
    private OrderPersistencePort orders;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Test
    @DisplayName("a customer's history lists newest first")
    void historyIsNewestFirst() {
        UUID customerId = UUID.randomUUID();
        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        Order oldest = persist(customerId, base.minus(2, ChronoUnit.HOURS));
        Order middle = persist(customerId, base.minus(1, ChronoUnit.HOURS));
        Order newest = persist(customerId, base);

        List<UUID> ids = idsOf(orders.findByCustomerId(customerId, PageQuery.of(0, 20)));

        assertThat(ids).containsExactly(newest.getId(), middle.getId(), oldest.getId());
    }

    @Test
    @DisplayName("the newest order is on the first page, not the last")
    void newestOrderLeadsTheFirstPage() {
        UUID customerId = UUID.randomUUID();
        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        persist(customerId, base.minus(2, ChronoUnit.HOURS));
        persist(customerId, base.minus(1, ChronoUnit.HOURS));
        Order newest = persist(customerId, base);

        PageResult<Order> firstPage = orders.findByCustomerId(customerId, PageQuery.of(0, 2));

        assertThat(firstPage.totalElements()).isEqualTo(3);
        assertThat(firstPage.totalPages()).isEqualTo(2);
        assertThat(idsOf(firstPage)).first().isEqualTo(newest.getId());
    }

    @Test
    @DisplayName("orders sharing a createdAt keep a stable order across pages")
    void tiedTimestampsPageWithoutLosingARow() {
        UUID customerId = UUID.randomUUID();
        Instant tie = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        // Four orders on the very same instant: without a tiebreaker the database is free to
        // order them differently per query, so a row can appear on both pages or on neither.
        for (int i = 0; i < 4; i++) {
            persist(customerId, tie);
        }

        // Asserted against the same query unpaged rather than against a hand-computed order:
        // the tiebreaker only has to be *consistent*, and the exact sequence is the database's
        // to choose. It does not match Java's — UUID.compareTo reads the high bits as a signed
        // long, while the uuid column collates unsigned, so the two disagree on half the values.
        List<UUID> unpaged = idsOf(orders.findByCustomerId(customerId, PageQuery.of(0, 4)));

        List<UUID> paged = Stream.concat(
                idsOf(orders.findByCustomerId(customerId, PageQuery.of(0, 2))).stream(),
                idsOf(orders.findByCustomerId(customerId, PageQuery.of(1, 2))).stream()
        ).toList();

        assertThat(unpaged).hasSize(4).doesNotHaveDuplicates();
        assertThat(paged).containsExactlyElementsOf(unpaged);
    }

    @Test
    @DisplayName("the admin listing is newest first too")
    void adminListingIsNewestFirst() {
        UUID customerId = UUID.randomUUID();
        Instant base = Instant.now().truncatedTo(ChronoUnit.SECONDS);

        persist(customerId, base.minus(1, ChronoUnit.HOURS));
        Order newest = persist(customerId, base.plus(1, ChronoUnit.HOURS));

        // Ordered against every order in the database, not just this customer's — the newest here
        // is an hour into the future so it leads regardless of what else the context seeded.
        List<UUID> ids = idsOf(orders.findAll(null, PageQuery.of(0, 20)));

        assertThat(ids).first().isEqualTo(newest.getId());
    }

    private List<UUID> idsOf(PageResult<Order> page) {
        return page.content().stream().map(Order::getId).toList();
    }

    /**
     * Writes an order with an exact {@code createdAt}. Checkout stamps {@code Instant.now()} and
     * offers no seam for it, and these assertions need timestamps that tie or sit hours apart.
     */
    private Order persist(UUID customerId, Instant createdAt) {
        Order order = Order.reconstitute(
                UUID.randomUUID(), customerId,
                List.of(OrderItem.of(UUID.randomUUID(), "Test Book", null, 1, BigDecimal.TEN)),
                OrderStatus.AWAITING_PAYMENT, BigDecimal.TEN,
                List.of(), createdAt, createdAt);
        return transactionTemplate.execute(tx -> orders.save(order));
    }
}
