package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JourneyTest {

    private static AccountEntity trip(long id, String name, String journey, String home) {
        AccountEntity a = new AccountEntity(name, "travel", false);
        a.id = id;
        a.journey = journey;
        a.homeCurrency = home;
        return a;
    }

    @Test
    public void groupsByNameIgnoringCaseAndSpaces() {
        AccountEntity vn = trip(1, "Vietnam", "SE Asia 2026", "THB");
        AccountEntity kh = trip(2, "Cambodia", " se asia  2026 ", "THB");
        AccountEntity jp = trip(3, "Japan", null, "THB");
        AccountEntity condo = new AccountEntity("Condo", "project", false);
        condo.journey = "SE Asia 2026";   // not a Travel account: ignored
        List<AccountEntity> all = Arrays.asList(vn, kh, jp, condo);

        assertEquals(Arrays.asList(vn, kh), Journey.members(all, "SE Asia 2026"));
        assertTrue(Journey.members(all, null).isEmpty());
        assertTrue(Journey.members(all, "  ").isEmpty());
        assertEquals(Arrays.asList("SE Asia 2026"), Journey.names(all));
        assertNull(Journey.clean("   "));
        assertEquals("SE Asia 2026", Journey.clean("  SE  Asia 2026 "));
        assertFalse(Journey.same(null, null));
    }

    @Test
    public void totalsOnlyAddTheSameHomeCurrency() {
        AccountEntity vn = trip(1, "Vietnam", "SE Asia", "THB");
        AccountEntity kh = trip(2, "Cambodia", "SE Asia", null);    // Settings currency
        AccountEntity cn = trip(3, "China", "SE Asia", "USD");      // kept separate
        Map<Long, Double> totals = new HashMap<>();
        totals.put(1L, 2500.10);
        totals.put(2L, 1200.20);
        totals.put(3L, 300.0);

        Journey.Total t = Journey.total(Arrays.asList(vn, kh, cn), totals, "THB", "THB");
        assertEquals(3700.30, t.amount, 1e-9);
        assertEquals(2, t.included);
        assertEquals(1, t.excluded);

        // A trip with no entries yet still counts as part of the journey
        totals.remove(2L);
        assertEquals(2, Journey.total(Arrays.asList(vn, kh), totals, "THB", "THB").included);
    }
}
