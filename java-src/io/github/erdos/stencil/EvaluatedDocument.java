package io.github.erdos.stencil;

import java.io.*;
import java.nio.channels.AsynchronousByteChannel;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.ExecutorService;
import java.util.function.Consumer;

/**
 * An evaluated document ready to be converted to the final output format.
 */
@SuppressWarnings("unused")
public interface EvaluatedDocument {

    TemplateDocumentFormats getFormat();

    Consumer<OutputStream> getWriter();

    /**
     * Writes output of this document to a file
     */
    default void writeToFile(File output) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(output)) {
            writeToStream(fos);
        }
    }

    default void writeToChannel(WritableByteChannel channel) throws IOException {
        try (OutputStream stream = Channels.newOutputStream(channel)) {
            writeToStream(stream);
        }
    }

    default void writeToChannel(AsynchronousByteChannel channel) throws IOException {
        try (OutputStream stream = Channels.newOutputStream(channel)) {
            writeToStream(stream);
        }
    }

    default void writeToStream(OutputStream outputStream) {
        getWriter().accept(outputStream);
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

        final long parentId = Thread.currentThread().getId();

        executorService.submit(() -> {
            final long childId = Thread.currentThread().getId();
            if (childId == parentId) {
                throw new IllegalStateException("The supplied executor must submit jobs to new threads!");
            } else {
                writeToStream(outputStream);
            }
        });

        return inputStream;
    }
}
