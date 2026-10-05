package com.devrenno.bookland.reviews.application.service;

import com.devrenno.bookland.reviews.application.port.out.TransactionPort;

import java.util.function.Supplier;

/** Pass-through transaction that also tells whether the code calling a port is inside it. */
class FakeTransactionPort implements TransactionPort {

    private boolean active;

    boolean isActive() {
        return active;
    }

    @Override
    public void inTransaction(Runnable work) {
        inTransaction(() -> {
            work.run();
            return null;
        });
    }

    @Override
    public <T> T inTransaction(Supplier<T> work) {
        active = true;
        try {
            return work.get();
        } finally {
            active = false;
        }
    }
}
