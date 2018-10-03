package io.github.erdos.stencil.impl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * This implementation deletes the file after calling close() method.
 */
@SuppressWarnings("unused")
public final class DeleteOnCloseFileInputStream extends FileInputStream {

    private final File file;

    public DeleteOnCloseFileInputStream(File file) throws FileNotFoundException {
        super(file);
        this.file = file;
    }

    @Override
    public void close() throws IOException {
        super.close();
        boolean success = file.delete();
    }
}
