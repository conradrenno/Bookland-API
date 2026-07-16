package com.devrenno.bookland.orders.infrastructure.transaction;

import com.devrenno.bookland.orders.application.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Spring-backed implementation of the framework-free TransactionPort, using a TransactionTemplate.
 * Explicit bean name avoids clashing with the wishlist module's adapter of the same simple name.
 */
@Component("ordersTransactionAdapter")
public class TransactionAdapter implements TransactionPort {

    private final TransactionTemplate transactionTemplate;

    public TransactionAdapter(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void inTransaction(Runnable work) {
        transactionTemplate.executeWithoutResult(status -> work.run());
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        return transactionTemplate.execute(status -> work.get());
    }
}
