package io.github.erdos.stencil;

import org.junit.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.github.erdos.stencil.TemplateData.fromMap;
import static io.github.erdos.stencil.TemplateVariables.fromPaths;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.Assert.fail;

public class TemplateVariablesTest {

    @Test
    public void testSimple() {
        final Set<String> schema = set("a.x.p", "a.x.q");
        Map<String, Object> data = map("a", map("x", map("p", null, "q", 23)));

        fromPaths(schema, emptySet()).throwWhenInvalid(fromMap(data));
    }


    @Test
    public void testArray() {
        final Set<String> schema = set("x[]");

        // valid
        final Map<String, Object> data = singletonMap("x", asList(1, 2, 3));
        fromPaths(schema, emptySet()).throwWhenInvalid(fromMap(data));

        try {
            // invalid
            final TemplateData data2 = fromMap(singletonMap("a", singletonMap("x", asList(1, 2, 3))));
            fromPaths(schema, emptySet()).throwWhenInvalid(data2);
            fail("Should have thrown!");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testNestedArray() {
        final Set<String> schema = set("a.x[][].p");

        // valid
        final Map<String, Object> data = singletonMap("a", singletonMap("x", singletonList(singletonList(singletonMap("p", 1)))));
        fromPaths(schema, emptySet()).throwWhenInvalid(fromMap(data));

        try {
            // invalid
            fromPaths(schema, emptySet()).throwWhenInvalid(fromMap(singletonMap("a", singletonMap("x", asList(1, 2, 3)))));
            fail("Should have thrown!");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @Test
    public void testNestedArraySimple() {
        final Set<String> schema = set("a.b.c.d");

        try {
            // invalid
            final Map<String, Object> data = singletonMap("a", 3);
            fromPaths(schema, emptySet()).throwWhenInvalid(fromMap(data));
            fail("Should have thrown!");
        } catch (IllegalArgumentException ignored) {
        }
    }

    @SafeVarargs
    private static <T> Set<T> set(T... elems) {
        return unmodifiableSet(new HashSet<>(asList(elems)));
    }

    private static <K, V> Map<K, V> map(K k1, V v1) {
        Map<K, V> m = new HashMap<>();
        m.put(k1, v1);
        return unmodifiableMap(m);
    }


    private static <K, V> Map<K, V> map(K k1, V v1, K k2, V v2) {
        Map<K, V> m = new HashMap<>();
        m.put(k1, v1);
        m.put(k2, v2);
        return unmodifiableMap(m);
    }
}