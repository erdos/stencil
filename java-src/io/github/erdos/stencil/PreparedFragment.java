package io.github.erdos.stencil;

public interface PreparedFragment extends AutoCloseable {

    /**
     * Makes the template clean up resources allocated for it.
     * <p>
     * Subsequent invocations of this method have no effects. Rendering the template after this method call will throw
     * an IllegalStateException.
     */
    @Override
    void close();
}
