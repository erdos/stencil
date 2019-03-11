package io.github.erdos.stencil.standalone;

import org.junit.Test;

import java.util.NoSuchElementException;
import java.util.Optional;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

public class ArgsParserTest {


    @Test
    public void testFlags() {
        // GIVEN
        ArgsParser parser = new ArgsParser();
        ArgsParser.ParamMarker<Boolean> m1 = parser.addFlagOption('f', "flag", "Example flag 1");
        ArgsParser.ParamMarker<Boolean> m2 = parser.addFlagOption('g', "gag", "Example flag 2");
        ArgsParser.ParamMarker<Boolean> m3 = parser.addFlagOption('b', "bag", "Example flag 3");


        // WHEN
        ArgsParser.ParseResult result = parser.parse("-g", "--flag", "--no-bag", "--", "file1", "file2");

        // THEN
        final Optional<Boolean> val = result.getParamValue(m1);
        assertTrue(val.isPresent());
        assertTrue(val.get());

        // THEN
        final Optional<Boolean> val2 = result.getParamValue(m2);
        assertTrue(val2.isPresent());
        assertTrue(val2.get());

        // THEN
        final Optional<Boolean> val3 = result.getParamValue(m3);
        assertTrue(val3.isPresent());
        assertFalse(val3.get());

        // THEN
        assertEquals(asList("file1", "file2"), result.getRestArgs());
    }

    @Test
    public void testArgLongForm() {
        // GIVEN
        ArgsParser parser = new ArgsParser();
        ArgsParser.ParamMarker<Integer> p = parser.addParam('n', "number", "a number", Integer::parseInt);

        // WHEN
        ArgsParser.ParseResult result = parser.parse("--number=100");

        // THEN
        final Optional<Integer> val = result.getParamValue(p);

        assertTrue(val.isPresent());
        assertEquals(Integer.valueOf(100), val.get());

        assertEquals(emptyList(), result.getRestArgs());
    }

    @Test
    public void testArgUnexpected() {
        // GIVEN
        ArgsParser parser = new ArgsParser();

        // WHEN-THEN
        try {
            parser.parse("--number=100");
            fail("expected exception!");
        } catch (NoSuchElementException ignore) {
            // intentional
        }

        // WHEN-THEN
        try {
            parser.parse("--number");
            fail("expected exception!");
        } catch (IllegalArgumentException ignore) {
            // intentional
        }

        // WHEN-THEN
        try {
            parser.parse("--no-number");
            fail("expected exception!");
        } catch (IllegalArgumentException ignore) {
            // intentional
        }

        // WHEN-THEN
        try {
            parser.parse("--number", "100");
            fail("expected exception!");
        } catch (IllegalArgumentException ignore) {
            // intentional
        }

        // WHEN-THEN
        try {
            parser.parse("-n");
            fail("expected exception!");
        } catch (IllegalArgumentException ignore) {
            // intentional
        }

        // WHEN-THEN
        try {
            parser.parse("-n", "100");
            fail("expected exception!");
        } catch (IllegalArgumentException ignore) {
            // intentional
        }
    }
}