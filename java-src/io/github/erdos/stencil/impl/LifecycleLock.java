package io.github.erdos.stencil.impl;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public final class LifecycleLock implements AutoCloseable {

    private final ReadWriteLock lock;
    private final AtomicBoolean alive;
    private final Runnable cleanup;

    public LifecycleLock(Runnable cleanup) {
        this.lock = new ReentrantReadWriteLock();
        this.alive = new AtomicBoolean(true);
        this.cleanup = Objects.requireNonNull(cleanup);
    }

    public <T> T execute(Callable<T> supplier) throws Exception {
        lock.readLock().lock();
        try {
            if (alive.get()) {
                return supplier.call();
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
