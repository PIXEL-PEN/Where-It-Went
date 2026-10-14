package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

public class TripCurrencyTest {

    @Test
    public void convertsDongAndYuan() {
        // 1 USD = 25,450 VND: a 500,000 dong dinner is $19.65
        assertEquals(19.65, TripCurrency.toHome(500_000, 25_450, "USD"), 0.0001);
        // 1 USD = 7.10 CNY: a 128.50 yuan ticket is $18.10
        assertEquals(18.10, TripCurrency.toHome(128.50, 7.10, "USD"), 0.0001);
        // 1 THB = 780 VND: 1,560,000 dong is 2,000 baht
        assertEquals(2000.00, TripCurrency.toHome(1_560_000, 780, "THB"), 0.0001);
    }

    @Test
    public void roundsHalfUpToHomeDecimals() {
        assertEquals(0.01, TripCurrency.toHome(0.5, 100, "USD"), 0.0001);     // 0.005 -> 0.01
        assertEquals(133.0, TripCurrency.toHome(20, 0.15, "JPY"), 0.0001);   // yen home: no decimals
    }

    @Test
    public void formatsWithTheRightDecimals() {
        assertEquals("500,000 ₫", TripCurrency.format(500_000, "VND"));
        assertEquals("128.50 ¥", TripCurrency.format(128.5, "CNY"));
        assertEquals("19.65 $", TripCurrency.format(19.65, "USD"));
        assertEquals("1,250 ¥", TripCurrency.format(1250, "JPY"));
        assertEquals("19.65 USD", TripCurrency.format(19.65, "USD", true));
    }

    @Test
    public void rateLabels() {
        assertEquals("1 USD = 25,450 VND", TripCurrency.rateLabel("USD", 25_450, "VND"));
        assertEquals("1 USD = 7.1 CNY", TripCurrency.rateLabel("USD", 7.10, "CNY"));
        assertEquals("1 THB = 0.2085 CNY", TripCurrency.rateLabel("THB", 0.2085, "CNY"));
    }

    @Test
    public void parsesRates() {
        assertEquals(25450.0, TripCurrency.parseRate("25,450"), 0.0001);
        assertEquals(7.1, TripCurrency.parseRate(" 7.10 "), 0.0001);
        assertNull(TripCurrency.parseRate("0"));
        assertNull(TripCurrency.parseRate("-3"));
        assertNull(TripCurrency.parseRate("abc"));
        assertNull(TripCurrency.parseRate(""));
    }

    @Test
    public void sameRateTolerance() {
        assertTrue(TripCurrency.sameRate(25450, 25450.000001));
        assertFalse(TripCurrency.sameRate(25450, 25500));
        assertFalse(TripCurrency.sameRate(0, 0));
    }

    @Test
    public void foreignTotalOnlyWhenEveryEntryHasOne() {
        assertEquals(628_500.0, TripCurrency.foreignTotal(Arrays.asList(500_000.0, 128_500.0)), 0.0001);
        assertEquals(0.3, TripCurrency.foreignTotal(Arrays.asList(0.1, 0.2)), 1e-12);   // no float drift
        assertNull(TripCurrency.foreignTotal(Arrays.asList(500_000.0, null)));
        assertNull(TripCurrency.foreignTotal(Collections.<Double>emptyList()));
    }

    @Test
    public void codesWhenSymbolsCollide() {
        assertTrue(TripCurrency.useCodes("USD", "CAD"));    // both "$"
        assertTrue(TripCurrency.useCodes("JPY", "CNY"));    // both "¥"
        assertFalse(TripCurrency.useCodes("USD", "VND"));
        assertFalse(TripCurrency.useCodes("THB", "CNY"));
    }

    @Test
    public void worldTable() {
        assertEquals(92, TripCurrency.ALL.size());
        java.util.Set<String> codes = new java.util.HashSet<>();
        for (TripCurrency.Currency c : TripCurrency.ALL) {
            assertTrue("duplicate " + c.code, codes.add(c.code));
            assertEquals(3, c.code.length());
            assertTrue(c.decimals >= 0 && c.decimals <= 3);
        }
        // The original Settings list keeps its order, so saved choices still match
        String[] original = {"USD","EUR","GBP","THB","JPY","CNY","INR","AUD","CAD","SGD","HKD","MYR","VND"};
        for (int i = 0; i < original.length; i++) {
            assertEquals(original[i], TripCurrency.ALL.get(i).code);
        }
        // Then alphabetical by code
        for (int i = TripCurrency.ORIGINAL_SETTINGS_COUNT + 1; i < TripCurrency.ALL.size(); i++) {
            assertTrue(TripCurrency.ALL.get(i - 1).code.compareTo(TripCurrency.ALL.get(i).code) < 0);
        }
        assertEquals("1.250 KD", TripCurrency.format(1.25, "KWD"));
        assertEquals("125,000 Rp", TripCurrency.format(125000, "IDR"));
        assertEquals("MXN — Mexican Peso ($)", TripCurrency.byCode("MXN").label());
    }

    @Test
    public void settingsSymbolLookups() {
        assertEquals("THB", TripCurrency.codeForSymbol("฿"));
        assertEquals("USD", TripCurrency.codeForSymbol("$"));
        assertEquals("VND", TripCurrency.codeForSymbol("₫"));
        assertEquals(12, TripCurrency.indexOf("VND"));
        assertEquals(-1, TripCurrency.indexOf("XYZ"));
    }
}
