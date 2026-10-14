package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ApproxRatesTest {

    @Test
    public void everyListedCurrencyHasARate() {
        for (TripCurrency.Currency c : TripCurrency.ALL) {
            assertTrue("no rate for " + c.code, ApproxRates.has(c.code));
        }
    }

    @Test
    public void crossRatesRoundedToFourFigures() {
        // 14 Oct 2026 table: 1 USD = 6.69182 CNY, 33.566 THB, 25900.5 VND
        assertEquals(6.692, ApproxRates.suggest("USD", "CNY"), 1e-9);
        assertEquals(771.6, ApproxRates.suggest("THB", "VND"), 1e-9);
        assertEquals(0.1494, ApproxRates.suggest("CNY", "USD"), 1e-9);
        assertEquals(25900.0, ApproxRates.suggest("USD", "VND"), 1e-9);
    }

    @Test
    public void unknownOrSameCurrency() {
        assertNull(ApproxRates.suggest("USD", "USD"));
        assertNull(ApproxRates.suggest("USD", "XYZ"));
        assertNull(ApproxRates.suggest(null, "CNY"));
        assertEquals("Oct 2026", ApproxRates.AS_OF_LABEL);
    }
}
