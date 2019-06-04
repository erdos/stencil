package io.github.erdos.stencil;

import io.github.erdos.stencil.impl.FileHelper;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;

public final class PreparedFragment implements AutoCloseable {

    private final Object content;
    private final File zipDirResource;
    private final AtomicBoolean alive = new AtomicBoolean(true);

    public PreparedFragment(Object content, File zipDirResource) {
        this.content = content;
        this.zipDirResource = zipDirResource;
    }

    public Object getImpl() {
        if (!alive.get()) {
            throw new IllegalStateException("Can not render destroyed fragment!");
        } else {
            return content;
        }
    }

    /**
     * Makes the template clean up any resources allocated for it.
     * <p>
     * Subsequent invocations of this method have no effects. Rendering the template after this method call will throw
     * an IllegalStateException.
     */
    public void cleanup() {
        if (alive.compareAndSet(true, false)) {
            FileHelper.forceDelete(zipDirResource);
        }
    }

    @Override
    public void close() {
        cleanup();
    }
}
