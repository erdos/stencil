package io.github.erdos.stencil.impl;

import org.slf4j.Logger;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Logging support
 */
@SuppressWarnings("WeakerAccess")
public final class Logging {

    private Logging() {}

    /**
     * Returns a consumer that can be used to print elapsed time.
     */
    public static Consumer<Supplier<String>> debugStopWatch(Logger logger) {
        AtomicLong lastMeasure = new AtomicLong(0);
        return msg -> {
            long now = System.currentTimeMillis();
            long previous = lastMeasure.getAndSet(now);

            if (previous == 0) {
                logger.debug(msg.get());
            } else
                logger.debug(msg.get(), now - previous);
        };
    }
}
