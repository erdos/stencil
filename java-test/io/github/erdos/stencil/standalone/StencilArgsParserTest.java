package io.github.erdos.stencil.standalone;

import io.github.erdos.stencil.standalone.ArgsParser.ParseResult;
import org.junit.Test;

import java.util.Optional;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.*;

public class StencilArgsParserTest {

    @Test
    public void parseEmpty() {
        ParseResult result = StencilArgsParser.parse();

        assertTrue(result.getRestArgs().isEmpty());
        assertOptionsUnset(result);
    }

    @Test
    public void parseFilesOnly1() {
        ParseResult result = StencilArgsParser.parse("file1", "file2", "file3");

        assertEquals(asList("file1", "file2", "file3"), result.getRestArgs());
        assertOptionsUnset(result);
    }

    @Test
    public void parseFilesOnly2() {
        ParseResult result = StencilArgsParser.parse("--", "file1", "file2", "file3");

        assertEquals(asList("file1", "file2", "file3"), result.getRestArgs());
        assertOptionsUnset(result);
    }

    @Test
    public void parseOverride() {
        final ParseResult result = StencilArgsParser.parse("--overwrite", "file1", "file2", "file3");
        final Optional<Boolean> overwriteFlag = result.getParamValue(StencilArgsParser.OVERWRITE);

        assertEquals(asList("file1", "file2", "file3"), result.getRestArgs());
        assertTrue(overwriteFlag.isPresent());
        assertTrue(overwriteFlag.get());
    }

    @Test(expected = IllegalArgumentException.class)
    public void parseOverrideError() {
        StencilArgsParser.parse("-overwrite");
    }

    @Test
    public void parseHelpAndVersion() {
        final ParseResult result = StencilArgsParser.parse("-h", "--version", "file1", "file2");
        final Optional<Boolean> helpFlag = result.getParamValue(StencilArgsParser.SHOW_HELP);
        final Optional<Boolean> versionFlag = result.getParamValue(StencilArgsParser.SHOW_VERSION);

        assertEquals(asList("file1", "file2"), result.getRestArgs());

        assertTrue(helpFlag.isPresent());
        assertTrue(helpFlag.get());

        assertTrue(versionFlag.isPresent());
        assertTrue(versionFlag.get());
    }

    private static void assertOptionsUnset(ParseResult result) {
        assertFalse(result.getParamValue(StencilArgsParser.OVERWRITE).isPresent());
        assertFalse(result.getParamValue(StencilArgsParser.JOBS_FILE).isPresent());
        assertFalse(result.getParamValue(StencilArgsParser.OUTPUT_DIR).isPresent());
        assertFalse(result.getParamValue(StencilArgsParser.SHOW_HELP).isPresent());
        assertFalse(result.getParamValue(StencilArgsParser.SHOW_VERSION).isPresent());
        assertFalse(result.getParamValue(StencilArgsParser.JOBS_FROM_STDIN).isPresent());
    }
}