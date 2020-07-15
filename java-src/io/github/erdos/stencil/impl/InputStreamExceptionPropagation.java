package io.github.erdos.stencil.impl;


import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Objects.requireNonNull;

public final class InputStreamExceptionPropagation extends FilterInputStream {
    private final AtomicReference<Throwable> exception = new AtomicReference<>(null);
    private final CountDownLatch complete = new CountDownLatch(1);

    public InputStreamExceptionPropagation(final InputStream stream) {
        super(stream);
    }

    @Override
    public void close() throws IOException {
        try {
            complete.await();
            final Throwable e = exception.get();
            if (e instanceof IOException) {
                throw (IOException) e;
            } else if (e != null) {
                throw new IOException("Exception while writing", e);
            }
        } catch (InterruptedException e) {
            throw new IOException("Interrupted!", e);
        } finally {
            in.close();
        }
    }

    public void fail(final Throwable e) {
        exception.set(requireNonNull(e));
    }

    public void finish() {
        complete.countDown();
    }
}