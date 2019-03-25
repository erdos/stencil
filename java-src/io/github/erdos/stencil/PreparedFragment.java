package io.github.erdos.stencil;

public final class PreparedFragment {

    private final Object content;

    public PreparedFragment(Object content) {
        this.content = content;
    }

    public Object getImpl() {
        return content;
    }
}
