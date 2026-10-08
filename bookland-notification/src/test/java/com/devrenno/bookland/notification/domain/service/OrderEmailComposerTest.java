package com.devrenno.bookland.notification.domain.service;

import com.devrenno.bookland.notification.domain.valueobject.EmailMessage;
import com.devrenno.bookland.notification.domain.valueobject.OrderEventKind;
import com.devrenno.bookland.notification.domain.valueobject.OrderLine;
import com.devrenno.bookland.notification.domain.valueobject.OrderNotice;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEmailComposerTest {

    private final OrderEmailComposer composer = new OrderEmailComposer();
    private final UUID orderId = UUID.fromString("3f2a9c4e-0000-4000-8000-000000000001");

    @ParameterizedTest
    @EnumSource(OrderEventKind.class)
    void everyKind_isAnEmailToTheCustomerWithTheOrderSummary(OrderEventKind kind) {
        EmailMessage email = composer.compose(notice(kind, "reader@bookland.com", "Ana", null)).orElseThrow();

        assertThat(email.to()).isEqualTo("reader@bookland.com");
        assertThat(email.subject()).contains("#3f2a9c4e");
        assertThat(email.body())
                .startsWith("Hello Ana,")
                .contains("2 x Clean Code — 29.90")
                .contains("Total: 59.80")
                .contains(orderId.toString());
    }

    @Test
    void subjectsTellTheKindsApart() {
        assertThat(subjectOf(OrderEventKind.CONFIRMED)).contains("confirmed");
        assertThat(subjectOf(OrderEventKind.PAYMENT_FAILED)).contains("not approved");
        assertThat(subjectOf(OrderEventKind.REJECTED)).contains("could not be placed");
        assertThat(subjectOf(OrderEventKind.SHIPPED)).contains("on its way");
        assertThat(subjectOf(OrderEventKind.CANCELLED)).contains("cancelled");
    }

    @Test
    void aFailedCheckout_saysWhy() {
        EmailMessage email = composer.compose(notice(OrderEventKind.PAYMENT_FAILED, "reader@bookland.com", "Ana",
                "Amount above the simulated card limit")).orElseThrow();

        assertThat(email.body()).contains("Reason: Amount above the simulated card limit");
    }

    @Test
    void noName_isAPlainGreeting() {
        EmailMessage email = composer.compose(notice(OrderEventKind.SHIPPED, "reader@bookland.com", null, null))
                .orElseThrow();

        assertThat(email.body()).startsWith("Hello,");
    }

    /** Orders placed before the email was stored on the order: nobody to write to. */
    @Test
    void noEmailAddress_isNoEmail() {
        assertThat(composer.compose(notice(OrderEventKind.CONFIRMED, null, "Ana", null))).isEmpty();
        assertThat(composer.compose(notice(OrderEventKind.CONFIRMED, " ", "Ana", null))).isEmpty();
    }

    private String subjectOf(OrderEventKind kind) {
        return composer.compose(notice(kind, "reader@bookland.com", "Ana", null)).orElseThrow().subject();
    }

    private OrderNotice notice(OrderEventKind kind, String email, String name, String reason) {
        return new OrderNotice(orderId, kind, email, name, new BigDecimal("59.80"),
                List.of(new OrderLine("Clean Code", 2, new BigDecimal("29.90"))), reason);
    }
}
