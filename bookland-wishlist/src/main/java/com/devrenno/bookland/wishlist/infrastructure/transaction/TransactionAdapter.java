package com.devrenno.bookland.wishlist.infrastructure.transaction;

import com.devrenno.bookland.wishlist.application.port.out.TransactionPort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

/**
 * Spring-backed implementation of the framework-free TransactionPort, using a TransactionTemplate.
 */
@Component
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
