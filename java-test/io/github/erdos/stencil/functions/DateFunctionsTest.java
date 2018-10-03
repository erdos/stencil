package io.github.erdos.stencil.functions;

import org.junit.Test;

import static io.github.erdos.stencil.functions.DateFunctions.DATE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateFunctionsTest {

    @Test
    public void testDate1() {

        // simple datetime format
        assertEquals("2017-01-01 08:11:12", DATE.call("yyyy-MM-dd HH:mm:ss", "2017-01-01T08:11:12"));

        // simple datetime format
        assertEquals("2001-07-04", DATE.call("yyyy-MM-dd", "2001-07-04 23:43:43"));


        // default LocalDateTime format
        assertEquals("2018-05-24", DATE.call("yyyy-MM-dd", "2018-05-24T11:49:52.520"));

        // default java.util.Date string format
        assertEquals("2018-05-24", DATE.call("yyyy-MM-dd", "Thu May 24 11:50:50 CEST 2018"));

        // default java.sql.Timestamp format
        assertEquals("2018-05-24", DATE.call("yyyy-MM-dd", "2018-05-24 22:33:44.345"));

    }

    @Test(expected = IllegalArgumentException.class)
    public void testNonParseable() {
        DATE.call("yyyy-MM-dd", "asdfsaf");
    }

    @Test
    public void testNullCases() {
        assertNull(DATE.call("yyyy-MM-dd", ""));
        assertNull(DATE.call("yyyy-MM-dd", null));
        assertNull(DATE.call(null, "2011-02-02"));
    }
}