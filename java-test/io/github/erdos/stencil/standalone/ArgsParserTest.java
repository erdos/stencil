package io.github.erdos.stencil.standalone;

import org.junit.Test;

import java.util.Optional;

import static java.util.Arrays.asList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;

public class ArgsParserTest {


    @Test
    public void testShortFlag() {
        // GIVEN
        ArgsParser parser = new ArgsParser();
        ArgsParser.ParamMarker<Boolean> m1 = parser.addFlagOption('f', "flag", "Example flag 1");

        // WHEN
        ArgsParser.ParseResult result = parser.parse("--flag", "--", "file1", "file2");

        // THEN
        final Optional<Boolean> val = result.getParamValue(m1);
        assertTrue(val.isPresent());
        assertTrue(val.get());

        assertEquals(asList("file1", "file2"), result.getRestArgs());
    }
}