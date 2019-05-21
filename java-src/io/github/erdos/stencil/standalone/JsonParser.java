package io.github.erdos.stencil.standalone;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.*;

/**
 * A naive implementation based on recursive descent parsing.
 */
public final class JsonParser {

    /**
     * Parses string and returns read object if any.
     */
    @SuppressWarnings({"unchecked", "unused", "WeakerAccess"})
    public static Object parse(String contents) throws IOException {
        return read(new StringReader(contents));
    }

    public static Object read(Reader reader) throws IOException {
        return simpleParse(new PushbackReader(reader, 128));
    }

    private static char peekNextNonWs(PushbackReader reader) throws IOException {
        // TODO: can optimize this with larger chunks.
        final char[] c = new char[1];
        while (true) {
            int l = reader.read(c, 0, 1);
            if (l == -1) {
                throw new IllegalStateException("Unexpected end of input!");
            } else if (Character.isWhitespace(c[0])) {
                //noinspection UnnecessaryContinue
                continue;
            } else {
                reader.unread(c[0]);
                return c[0];
            }
        }
    }

    private static Object simpleParse(PushbackReader pb) throws IOException {
        char c = peekNextNonWs(pb);
        if (c == '{') {
            return readMap(pb);
        } else if (c == '[') {
            return readVec(pb);
        } else if (c == '"') {
            return readStr(pb);
        } else if (c == 't') {
            expectWord("true", pb);
            return Boolean.TRUE;
        } else if (c == 'f') {
            expectWord("false", pb);
            return Boolean.FALSE;
        } else if (c == 'n') {
            expectWord("null", pb);
            return null;
        } else if (Character.isDigit(c)) {
            return readNumber(pb);
        } else {
            throw new IllegalStateException("Unexpected character: '" + c + "'");
        }
    }

    static Number readNumber(PushbackReader pb) throws IOException {
        char[] arr = new char[128];
        int len = pb.read(arr, 0, arr.length);

        assert Character.isDigit(arr[0]);

        int i = 0;
        while (i < len && (arr[i] == '.' || arr[i] == '-' || arr[i] == 'e' || Character.isDigit(arr[i]))) {
            i++;
        }

        if (i < len) {
            pb.unread(arr, i, len - i);
        }
        try {
            return new BigDecimal(arr, 0, i);
        } catch (NumberFormatException e) {
            throw new IllegalStateException("Could not parse '" + new String(arr, 0, i) + "'", e);
        }
    }

    static String readStr(PushbackReader pb) throws IOException {
        expectWord("\"", pb);

        final StringBuffer buf = new StringBuffer();
        while (true) {
            final int read = pb.read();

            if (read == -1) {
                throw new IllegalStateException("Unexpected end of file!");
            } else if (read == '\\') {
                int c2 = pb.read();
                if (c2 == '\\' || c2 == '"') {
                    buf.append((char) c2);
                } else if (c2 == 'n') {
                    buf.append('\n');
                } else if (c2 == 't') {
                    buf.append('\t');
                } else if (c2 == 'r') {
                    buf.append('\r');
                } else {
                    throw new IllegalStateException("Unexpected character: \\" + ((char) c2));
                }
            } else if (read == '"') {
                return buf.toString();
            } else {
                buf.append((char) read);
            }
        }
    }

    static void expectWord(String word, PushbackReader pb) throws IOException {
        final char[] c = new char[word.length()];
        if (!(pb.read(c, 0, c.length) == c.length && Arrays.equals(c, word.toCharArray()))) {
            throw new IllegalStateException("Expected: " + word + " but found " + new String(c) + " instead.");
        }
    }

    static List<Object> readVec(PushbackReader pb) throws IOException {
        expectWord("[", pb);
        final List<Object> buf = new ArrayList<>();
        while (true) {
            char c = peekNextNonWs(pb);
            if (c == ']') {
                pb.read();
                return Collections.unmodifiableList(buf);
            } else if (c == ',') {
                pb.read();
            } else {
                final Object obj = simpleParse(pb);
                buf.add(obj);
            }
        }
    }

    static Object readMap(PushbackReader pb) throws IOException {
        expectWord("{", pb);

        final Map<String, Object> buf = new HashMap<>();
        while (true) {
            char c = peekNextNonWs(pb);
            if (c == '}') {
                pb.read();
                return Collections.unmodifiableMap(buf);
            } else {
                final String key = readStr(pb);
                char sep = peekNextNonWs(pb);
                if (sep != ':') {
                    throw new IllegalStateException("Unexpected character " + sep + ", expected ':'");
                }
                pb.read();

                final Object val = simpleParse(pb);

                buf.put(key, val);

                final char c2 = peekNextNonWs(pb);
                if (c2 == '}') {
                    continue;
                } else if (c2 == ',') {
                    pb.read();
                    continue;
                } else {
                    throw new IllegalStateException("Unexpected character after key-value pair in JSON: '" + c2 + "'");
                }
            }
        }
    }
}