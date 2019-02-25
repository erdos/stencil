package io.github.erdos.stencil.standalone;

import org.junit.Test;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class JsonParserTest {

    @Test
    public void testReadStr() throws IOException {
        final String input = "\"asdf\"1";
        PushbackReader pbr = pbr(input);
        final String out = JsonParser.readStr(pbr);

        assertEquals("asdf", out);
        assertEquals('1', pbr.read());
    }

    @Test
    public void expectWordTest() throws IOException {
        final String input = "Alabama";
        JsonParser.expectWord(input, pbr(input));
    }

    @Test(expected = IllegalStateException.class)
    public void expectWordFailureTest() throws IOException {
        final String input = "Alabama";
        JsonParser.expectWord("Alibaba", pbr(input));
    }

    @Test(expected = IllegalStateException.class)
    public void expectWordFailureTest2() throws IOException {
        final String input = "Alab";
        JsonParser.expectWord("Alabama", pbr(input));
    }

    @Test
    public void readVecTestEmpty() throws IOException {
        final String input = "[]";
        final Object result = JsonParser.readVec(pbr(input));
        assertEquals(emptyList(), result);
    }

    @Test
    public void readVecTestUnit() throws IOException {
        final String input = "[1]";
        final Object result = JsonParser.readVec(pbr(input));
        assertEquals(singletonList(BigDecimal.ONE), result);
    }

    @Test
    public void readVecTestSimple() throws IOException {
        final String input = "[1,10]";
        final Object result = JsonParser.readVec(pbr(input));
        assertEquals(asList(BigDecimal.ONE, BigDecimal.TEN), result);
    }

    @Test
    public void readMapTestEmpty() throws IOException {
        assertEquals(emptyMap(), JsonParser.readMap(pbr("{}")));
        assertEquals(emptyMap(), JsonParser.readMap(pbr("{     }")));
        assertEquals(emptyMap(), JsonParser.readMap(pbr("{}x")));
        assertEquals(emptyMap(), JsonParser.readMap(pbr("{ }y")));
    }

    @Test
    public void readMapTestSimple() throws IOException {
        assertEquals(singletonMap("a", BigDecimal.ONE), JsonParser.readMap(pbr("{\"a\": 1}")));
        assertEquals(singletonMap("b", singletonList(BigDecimal.ONE)), JsonParser.readMap(pbr("{\"b\": [1]}")));
    }

    @Test
    public void readMapTestComplex() throws IOException {
        final Map<String, Object> expected = new HashMap<>();
        expected.put("a", BigDecimal.ONE);
        expected.put("b", singletonList("c"));

        final String input = "{\"a\": 1, \"b\":[\"c\"]}";

        assertEquals(expected, JsonParser.readMap(pbr(input)));
    }

    @Test
    public void readNumberTest() throws IOException {
        final String input = "123456789.123456789";
        final Number result = JsonParser.readNumber(pbr(input));
        assertEquals(new BigDecimal(input), result);
    }

    @Test
    public void readNumberTest2() throws IOException {
        final String input = "123456789.123456789xyz";
        PushbackReader pbr = pbr(input);

        final Number result = JsonParser.readNumber(pbr);
        assertEquals(new BigDecimal("123456789.123456789"), result);
        assertEquals((Character) 'x', (Character) (char) pbr.read());
    }

    @Test
    public void readScalarsTest() throws IOException {
        assertEquals(true, JsonParser.read(pbr("true")));
        assertEquals(false, JsonParser.read(pbr("false")));
        assertNull(JsonParser.read(pbr("null")));
    }

    private static PushbackReader pbr(String s) {
        return new PushbackReader(new StringReader(s), 32);
    }
}
