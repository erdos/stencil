package io.github.erdos.stencil.functions;

import org.junit.Test;

import static io.github.erdos.stencil.functions.LocaleFunctions.CURRENCY;
import static io.github.erdos.stencil.functions.LocaleFunctions.PERCENT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LocaleFunctionsTest {

    @Test
    public void testCurrency() {
        assertNull(CURRENCY.call(new Object[]{null}));
        assertEquals("123,00 Ft", CURRENCY.call(123, "hu_HU"));
        assertEquals("123,45 Ft", CURRENCY.call(123.45, "hu_HU"));
        assertEquals("0,00 Ft", CURRENCY.call(0.0, "hu_HU"));
        assertEquals("-1,00 Ft", CURRENCY.call(-1, "hu_HU"));
    }

    @Test
    public void testPercent() {
        assertNull(PERCENT.call(new Object[]{null}));
        assertEquals("12 300%", PERCENT.call(123, "HU-HU"));
        assertEquals("95%", PERCENT.call(0.95, "HU-HU"));
        assertEquals("1%", PERCENT.call(0.0095, "HU-HU"));
        assertEquals("-200%", PERCENT.call(-2, "HU-HU"));
    }
}