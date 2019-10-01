package io.github.erdos.stencil.scenarios;

import org.junit.Test;
import io.github.erdos.stencil.API;
import io.github.erdos.stencil.EvaluatedDocument;
import io.github.erdos.stencil.PreparedTemplate;
import io.github.erdos.stencil.TemplateData;
import io.github.erdos.stencil.impl.ZipHelper;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.joining;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertFalse;

public class TableColumnsTest {

    /**
     * All table columns are visible.
     */
    @Test
    public void testAllColumnsVisible() {
        Map<String, Object> data = new HashMap<>();

        data.put("testerName", "Wizard");
        data.put("col1", true);
        data.put("col2", true);
        data.put("col3", true);
        data.put("col4", true);

        testWithData("test-table-columns.docx",
                data,
                asList("Hello Wizard!",
                        "A1", "A2", "A3", "A4",
                        "B1+B2", "B3", "B4",
                        "C1", "C2+C3", "C4",
                        "D1", "D2", "D3+D4",
                        "E1+E2+E3", "E4",
                        "F1", "F2+F3+F4",
                        "G1+G2+G3+G4"),
                emptyList());
    }

    /**
     * Hiding first column must not affect other columns.
     */
    @Test
    public void testHidingColumn1() {
        Map<String, Object> data = new HashMap<>();

        data.put("testerName", "Genius");
        data.put("col1", false);
        data.put("col2", true);
        data.put("col3", true);
        data.put("col4", true);

        testWithData("test-table-columns.docx",
                data,
                asList("Hello Genius!",
                        "A2", "A3", "A4",
                        "B1+B2", "B3", "B4",
                        "C2+C3", "C4",
                        "D2", "D3+D4",
                        "E1+E2+E3", "E4",
                        "F2+F3+F4",
                        "G1+G2+G3+G4"),
                asList("A1", "C1", "D1", "F1"));
    }

    /**
     * Hiding second column must not affect other columns.
     */
    @Test
    public void testHidingColumn2() {
        Map<String, Object> data = new HashMap<>();

        data.put("testerName", "Wizard");
        data.put("col1", true);
        data.put("col2", false);
        data.put("col3", true);
        data.put("col4", true);

        testWithData("test-table-columns.docx",
                data,
                asList("Hello Wizard!",
                        "A1", "A3", "A4",
                        "B1+B2", "B3", "B4",
                        "C1", "C2+C3", "C4",
                        "D1", "D3+D4",
                        "E1+E2+E3", "E4",
                        "F1", "F2+F3+F4",
                        "G1+G2+G3+G4"),
                asList("A2", "D2"));
    }

    /**
     * Hiding all columns must remove all.
     */
    @Test
    public void testHidingColumnAll() {
        Map<String, Object> data = new HashMap<>();

        data.put("testerName", "Wizard");
        data.put("col1", false);
        data.put("col2", false);
        data.put("col3", false);
        data.put("col4", false);

        testWithData("test-table-columns.docx",
                data,
                asList("Hello Wizard!", "Some text after the table"),
                asList("A2", "B1+B2", "D2", "E1+E2+E3", "F2+F3+F4", "G1+G2+G3+G4"));
    }

    /**
     * Unit test implementation.
     *
     * @param testFileName   file name of template file.
     * @param data           template data.
     * @param mustContain    the result must contain these words.
     * @param mustNotContain the result must not contains these words.
     */
    @SuppressWarnings("SameParameterValue")
    public static void testWithData(String testFileName, Map<String, Object> data, List<String> mustContain, List<String> mustNotContain) {
        try {
            final URL testFileUrl = TableColumnsTest.class.getClassLoader().getResource(testFileName);

            assertNotNull(testFileUrl);

            final File testFile = Paths.get(testFileUrl.toURI()).toFile();
            assertTrue(testFile.exists());

            final PreparedTemplate prepared = API.prepare(testFile);
            final EvaluatedDocument result = API.render(prepared, TemplateData.fromMap(data));

            final File temporaryDocx = File.createTempFile("sdf", ".docx");
            assertTrue(temporaryDocx.delete());
            result.writeToFile(temporaryDocx);

            System.out.println("Temporary docx file: " + temporaryDocx);

            final File tmpdir = File.createTempFile("stencil-test", "");
            assertTrue(tmpdir.delete()); // so that we can create directory
            tmpdir.deleteOnExit();

            try (InputStream docxStream = new FileInputStream(temporaryDocx)) {
                ZipHelper.unzipStreamIntoDirectory(docxStream, tmpdir);
            }

            final String allWords = tree(tmpdir)
                    .stream()
                    .filter(x -> x.getName().endsWith(".xml"))
                    .map(TableColumnsTest::extractWords)
                    .collect(joining(" "));

            mustContain.forEach(w -> assertTrue("Should contain " + w, allWords.contains(w)));
            mustNotContain.forEach(w -> assertFalse("Should not contain " + w, allWords.contains(w)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns all files from a directory recursively.
     */
    private static List<File> tree(File f) {
        assertTrue(f.exists());
        assertTrue(f.isDirectory());

        //noinspection ConstantConditions
        return Arrays.stream(f.listFiles()).flatMap(d -> {
            if (d.isDirectory()) return tree(d).stream();
            else return Stream.of(d);
        }).collect(Collectors.toList());
    }

    /**
     * Returns a concatenation of all string literals from an xml file.
     *
     * @param xmlFile an existing XML file
     * @return string of text contents
     */
    private static String extractWords(File xmlFile) {
        assertTrue(xmlFile.getName().endsWith(".xml"));
        assertTrue(xmlFile.exists());

        final StringBuilder buffer = new StringBuilder();
        try (InputStream inputStream = new FileInputStream(xmlFile)) {
            final XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(inputStream);

            while (reader.hasNext()) {
                final int next = reader.next();

                if (next == XMLStreamReader.CHARACTERS || next == XMLStreamReader.SPACE) {
                    buffer.append(reader.getText());
                }
            }
            reader.close();
            return buffer.toString();
        } catch (IOException | XMLStreamException e) {
            throw new RuntimeException(e);
        }
    }
}
