package io.github.erdos.stencil;

public interface PreparedFragment extends AutoCloseable {

    /**
     * Implementation detail.
     */
    @Deprecated
    Object getImpl();

    /**
     * Makes the template clean up any resources allocated for it.
     * <p>
     * Subsequent invocations of this method have no effects. Rendering the template after this method call will throw
     * an IllegalStateException.
     */
    @Override
    void close();
}
