package io.github.erdos.stencil.impl;

import java.util.*;
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

    public static <T> T execute(List<LifecycleLock> locks, Callable<T> supplier) throws Exception {
        if (locks.isEmpty()) {
            return supplier.call();
        } else {
            ListIterator<LifecycleLock> iterator = locks.listIterator();
            try {
                while (iterator.hasNext()) {
                    iterator.next().acquireReadLockAndCheck();
                }
                return supplier.call();
            } finally {
                while (iterator.hasPrevious()) {
                    iterator.previous().lock.readLock().unlock();
                }
            }
        }
    }

    private void acquireReadLockAndCheck() {
        lock.readLock().lock();
        if (!alive.get()) { // we DO NOT release read lock, because it will be released in finally block!
            throw new IllegalStateException("Component has already been closed.");
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
