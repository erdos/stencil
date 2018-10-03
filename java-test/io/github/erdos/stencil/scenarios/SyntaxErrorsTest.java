package io.github.erdos.stencil.scenarios;

import io.github.erdos.stencil.API;
import io.github.erdos.stencil.exceptions.ParsingException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;
import static junit.framework.TestCase.assertTrue;

public class SyntaxErrorsTest {


    @Test(expected = ParsingException.class)
    public void testSyntaxError1() throws URISyntaxException, IOException {
        final String testFileName = "failures/test-syntax-error-1.docx";
        final URL testFileUrl = requireNonNull(TableColumnsTest.class.getClassLoader().getResource(testFileName));
        final File testFile = Paths.get(testFileUrl.toURI()).toFile();

        assertTrue(testFile.exists());

        // this should throw an error.
        API.prepare(testFile);
    }
}
