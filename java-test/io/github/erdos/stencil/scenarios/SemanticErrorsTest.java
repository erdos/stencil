package io.github.erdos.stencil.scenarios;

import io.github.erdos.stencil.API;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import io.github.erdos.stencil.exceptions.EvalException;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;

import static java.util.Objects.requireNonNull;
import static junit.framework.TestCase.assertTrue;

public class SemanticErrorsTest {

    @Test(expected = EvalException.class)
    public void testSyntaxError1() throws URISyntaxException, IOException {
        final String testFileName = "failures/test-semantic-error-1.docx";


        final URL testFileUrl = requireNonNull(TableColumnsTest.class.getClassLoader().getResource(testFileName));
        final File testFile = Paths.get(testFileUrl.toURI()).toFile();

        assertTrue(testFile.exists());

        // this should throw an error.
        PreparedTemplate preparedTemplate = API.prepare(testFile);


        API.render(preparedTemplate, TemplateData.empty());
    }
}
