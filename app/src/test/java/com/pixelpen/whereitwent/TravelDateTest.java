package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Calendar;

public class TravelDateTest {

    private static long at(int y, int m, int d, int h) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m - 1, d, h, 30);
        return c.getTimeInMillis();
    }

    @Test
    public void todayFollowsTheCalendar() {
        long monEvening = at(2026, 10, 12, 21);
        long tueMorning = at(2026, 10, 13, 8);
        // Left on today (Monday): the next morning shows Tuesday
        assertFalse(TravelDate.isPinned(monEvening, monEvening));
        assertEquals(TravelDate.midnight(tueMorning),
                TravelDate.resolve(TravelDate.midnight(monEvening), false, tueMorning));
    }

    @Test
    public void aPickedDateIsKept() {
        long sunday = at(2026, 10, 11, 12);
        long tueMorning = at(2026, 10, 13, 8);
        assertTrue(TravelDate.isPinned(sunday, at(2026, 10, 12, 21)));
        assertEquals(TravelDate.midnight(sunday), TravelDate.resolve(sunday, true, tueMorning));
        // Nothing stored yet: today
        assertEquals(TravelDate.midnight(tueMorning), TravelDate.resolve(0, true, tueMorning));
    }
}
