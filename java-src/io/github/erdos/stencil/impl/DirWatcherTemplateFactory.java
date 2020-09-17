package io.github.erdos.stencil.impl;

import io.github.erdos.stencil.PrepareOptions;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;

import static java.lang.System.currentTimeMillis;
import static java.nio.file.StandardWatchEventKinds.*;
import static java.util.Arrays.stream;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

/**
 * Watches file system and automatically loads template files on file changes.
 */
@SuppressWarnings("unused")
public final class DirWatcherTemplateFactory implements TemplateFactory {

    private final File templatesDirectory;
    private final TemplateFactory factory;

    private final DelayQueue<DelayedContainer<File>> delayQueue = new DelayQueue<>();
    private final Map<File, DelayedContainer<File>> delays = new ConcurrentHashMap<>();
    private final AtomicBoolean running = new AtomicBoolean(false);

    /**
     * Default ctor.
     *
     * @param templatesDirectory not null absolute path directory
     * @param factory            wrapped factory
     */
    @SuppressWarnings("WeakerAccess")
    public DirWatcherTemplateFactory(File templatesDirectory, TemplateFactory factory) {
        if (templatesDirectory == null)
            throw new IllegalArgumentException("Template directory parameter is null!");
        if (!templatesDirectory.exists())
            throw new IllegalArgumentException("Templates directory does not exist: " + templatesDirectory);
        if (!templatesDirectory.isDirectory())
            throw new IllegalArgumentException("Templates directory parameter is not a directory!");
        if (factory == null)
            throw new IllegalArgumentException("Parent factory is missing!");

        this.templatesDirectory = templatesDirectory;
        this.factory = factory;
    }

    public File getTemplatesDirectory() {
        return templatesDirectory;
    }

    private Optional<PreparedTemplate> handle(File f) {
        assert (f.isAbsolute());

        try {
            final PreparedTemplate template = factory.prepareTemplateFile(f, PrepareOptions.options());
            // TODO: we may use logging here
            return Optional.of(template);
        } catch (IOException e) {
            e.printStackTrace();
            return Optional.empty();
        }
    }

    /**
     * Starts directory watcher and tries to load all files.
     *
     * @throws IOException           on file system errors
     * @throws IllegalStateException if already started
     */
    public void start() throws IOException, IllegalStateException {
        if (running.getAndSet(true))
            throw new IllegalStateException("Already running!");

        Path path = templatesDirectory.toPath();
        final WatchService ws = path.getFileSystem().newWatchService();
        final WatchKey waka = path.register(ws, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);

        new Thread(() -> {
            try {
                initAllFiles();
            } catch (Exception ignored) {
                // intentionally left blank
            }

            try {
                while (running.get()) {
                    if (delayQueue.isEmpty()) {
                        addEvents(ws.take());
                    }

                    List<DelayedContainer<File>> elems = new LinkedList<>();
                    if (0 < delayQueue.drainTo(elems)) {
                        elems.forEach(x -> {
                            delays.remove(x.getElem());
                            handle(x.getElem());
                        });
                    } else {
                        long delay = delayQueue.peek().getDelay(TimeUnit.MILLISECONDS);
                        WatchKey poll = ws.poll(delay, TimeUnit.MILLISECONDS);
                        if (poll != null) {
                            addEvents(poll);

                        }
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void initAllFiles() {
        recurse(templatesDirectory).forEach(this::handle);
    }

    private Stream<File> recurse(File f) {
        if (f != null && f.isDirectory()) {
            final String[] files = f.list();
            if (files == null) {
                return Stream.empty();
            } else {
                return stream(files).map(x -> new File(f, x)).flatMap(this::recurse);
            }
        } else {
            return Stream.empty();
        }
    }

    @SuppressWarnings("unchecked")
    private void addEvents(WatchKey key) {
        assert (key != null);
        for (WatchEvent<?> event : key.pollEvents()) {
            final WatchEvent<Path> ev = (WatchEvent<Path>) event;
            final File f = new File(templatesDirectory, ev.context().toFile().getName());
            if (delays.containsKey(f)) {
                DelayedContainer<File> container = delays.get(f);
                delays.remove(f);
                delayQueue.remove(container);
            }

            final DelayedContainer<File> newCont = new DelayedContainer<>(1000L, f);
            delays.put(f, newCont);
            delayQueue.add(newCont);
        }
        key.reset();
    }

    @SuppressWarnings("WeakerAccess")
    public void stop() {
        if (!running.getAndSet(false))
            throw new IllegalStateException("Already stopped!");
        delays.clear();
        delayQueue.clear();
    }

    @Override
    public PreparedTemplate prepareTemplateFile(File inputTemplateFile, PrepareOptions options) {
        if (inputTemplateFile == null)
            throw new IllegalArgumentException("templateFile argument must not be null!");
        if (inputTemplateFile.isAbsolute())
            throw new IllegalArgumentException("templateFile must not be an absolute file!");
        else
            return handle(inputTemplateFile)
                    .orElseThrow(() -> new IllegalArgumentException("Can not build template file: " + inputTemplateFile));
    }

    private final class DelayedContainer<X> implements Delayed {
        private final long expiration;
        private final X contents;

        private DelayedContainer(long millis, X contents) {
            if (millis <= 0)
                throw new IllegalArgumentException("Millis must be positive!");
            this.expiration = currentTimeMillis() + millis;
            this.contents = contents;
        }

        private X getElem() {
            return contents;
        }

        public String toString() {
            return "D+" + expiration;
        }

        @Override
        public long getDelay(TimeUnit unit) {
            return unit.convert(expiration - currentTimeMillis(), MILLISECONDS);
        }

        @Override
        public int compareTo(Delayed delayed) {
            return (int) (getDelay(MILLISECONDS) - delayed.getDelay(MILLISECONDS));
        }
    }
}
