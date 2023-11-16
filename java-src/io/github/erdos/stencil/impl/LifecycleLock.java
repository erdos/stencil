package io.github.erdos.stencil.impl;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

public final class LifecycleLock implements AutoCloseable {

    private final ReentrantReadWriteLock lock;
    private final AtomicBoolean alive;
    private final Runnable cleanup;

    public LifecycleLock(Runnable cleanup) {
        this.lock = new ReentrantReadWriteLock();
        this.alive = new AtomicBoolean(true);
        this.cleanup = Objects.requireNonNull(cleanup);
    }

    public <T> T run(Supplier<T> supplier) {
        lock.readLock().lock();
        try {
            if (alive.get()) {
                return supplier.get();
            } else {
                throw new IllegalStateException("Component has already been closed.");
            }
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void close() throws Exception {
        if (alive.get()) {
            lock.writeLock().lock();
            try {
                if (alive.getAndSet(false)) {
                    cleanup.run();
                }
            } finally {
                lock.writeLock().unlock();
            }
        }
    }
}
