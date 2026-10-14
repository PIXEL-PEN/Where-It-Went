package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class CurrencySearchTest {

    private static List<String> codes(String q) {
        List<String> out = new ArrayList<>();
        for (int i : TripCurrency.search(q)) out.add(TripCurrency.ALL.get(i).code);
        return out;
    }

    @Test
    public void findsByCountryCurrencyNameOrCode() {
        assertEquals("[KHR]", codes("camb").toString());
        assertEquals("[KHR]", codes("Riel").toString());
        assertEquals("[KHR]", codes(" khr ").toString());
        assertTrue(codes("viet").contains("VND"));
        assertTrue(codes("laos").contains("LAK"));
        assertTrue(codes("korea").contains("KRW"));
        assertTrue(codes("usa").contains("USD"));
        assertTrue(codes("france").contains("EUR"));
        assertTrue(codes("new zealand").contains("NZD"));
    }

    @Test
    public void blankShowsAllInListOrder() {
        List<Integer> all = TripCurrency.search("");
        assertEquals(TripCurrency.ALL.size(), all.size());
        assertEquals(0, (int) all.get(0));
        // Several dollars, original Settings ones first
        List<String> dollars = codes("dollar");
        assertEquals("USD", dollars.get(0));
        assertTrue(dollars.size() > 8);
        assertTrue(codes("zzz").isEmpty());
    }
}
