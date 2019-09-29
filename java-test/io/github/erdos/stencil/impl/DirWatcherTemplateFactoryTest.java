package io.github.erdos.stencil.impl;

import io.github.erdos.stencil.PrepareOptions;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateFactory;
import io.github.erdos.stencil.TemplateVariables;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DirWatcherTemplateFactoryTest implements TemplateFactory {

    private final Set<File> calledFiles = new HashSet<>();
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private DirWatcherTemplateFactory factory;
    private File folder;

    @Before
    public void cleanup() throws IOException {
        calledFiles.clear();
        folder = temporaryFolder.newFolder();
        factory = new DirWatcherTemplateFactory(folder, this);
        factory.start();
    }

    @After
    public void after() {
        factory.stop();
    }

    @Test
    public void testFileChange() throws IOException, InterruptedException {
        File file1 = makeFile("asd");
        assertFalse(calledFiles.contains(file1));
        Thread.sleep(1100L);
        assertTrue(calledFiles.contains(file1));
    }

    @Test
    public void testFileChange2() throws IOException, InterruptedException {
        Thread.sleep(2000L);
        File file1 = makeFile("asd");
        assertFalse(calledFiles.contains(file1));

        Thread.sleep(200L);
        makeFile("asd");
        File file2 = makeFile("fedfed");

        Thread.sleep(600L);
        assertFalse(calledFiles.contains(file1));

        makeFile("asd");
        Thread.sleep(600L);
        assertFalse(calledFiles.contains(file1));
        assertTrue(calledFiles.contains(file2));

        makeFile("asd");
        Thread.sleep(1100L);
        assertTrue(calledFiles.contains(file1));
    }


    private File makeFile(String fname) throws IOException {
        final File file1 = new File(folder, fname).getAbsoluteFile();
        try (OutputStream fo = new FileOutputStream(file1)) {
            fo.write("Hello".getBytes());
        }
        return file1;
    }

    @Override
    public PreparedTemplate prepareTemplateFile(File inputTemplateFile, PrepareOptions options) {
        calledFiles.add(inputTemplateFile);
        return new PreparedTemplate() {

            @Override
            public File getTemplateFile() {
                return inputTemplateFile;
            }

            @Override
            public LocalDateTime creationDateTime() {
                return null;
            }

            @Override
            public Object getSecretObject() {
                return null;
            }

            @Override
            public TemplateVariables getVariables() {
                return null;
            }

            @Override
            public void cleanup() {
                // NOP

            }
        };
    }
}
