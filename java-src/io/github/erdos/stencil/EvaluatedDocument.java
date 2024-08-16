package io.github.erdos.stencil;

import io.github.erdos.stencil.impl.InputStreamExceptionPropagation;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;

/**
 * An evaluated document ready to be converted to the final output format.
 */
@SuppressWarnings("unused")
public interface EvaluatedDocument {

    void write(OutputStream target);

    /**
     * Writes output of this document to a file
     */
    default void writeToFile(File output) throws IOException {
        writeToPath(output.toPath());
    }

    /**
     * Writes output of this document to a path
     */
    default void writeToPath(Path output) throws IOException {
        if (Files.exists(output)) {
            throw new IllegalArgumentException("Output path already exists: " + output);
        }
        try (OutputStream fos = Files.newOutputStream(output)) {
            write(fos);
        }
    }

    /**
     * Creates a blocking input stream that can be used to render generated document.
     *
     * @param executorService used to stream output.
     * @return a new input stream that contains the generated document
     * @throws NullPointerException  if executorService is null
     * @throws IllegalStateException if executorService would use the current thread
     */
    default InputStream toInputStream(ExecutorService executorService) {
        final PipedOutputStream outputStream = new PipedOutputStream();
        final PipedInputStream inputStream = new PipedInputStream();
        final InputStreamExceptionPropagation inputStreamErrors = new InputStreamExceptionPropagation(inputStream);

        try {
            inputStream.connect(outputStream);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }

        final long parentId = Thread.currentThread().getId();

        executorService.submit(() -> {
            final long childId = Thread.currentThread().getId();
            if (childId == parentId) {
                throw new IllegalStateException("The supplied executor must submit jobs to new threads!");
            } else {
                try {
                    write(outputStream);
                } catch (Throwable e) {
                    inputStreamErrors.fail(e);
                    throw e;
                } finally {
                    inputStreamErrors.finish();
                }
            }
        });

        return inputStreamErrors;
    }
}
