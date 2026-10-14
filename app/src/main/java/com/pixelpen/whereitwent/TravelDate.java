package com.pixelpen.whereitwent;

import java.util.Calendar;

/**
 * Travel mode's remembered entry date (v3.3). Pure Java, unit-tested.
 *
 * A date the person picked (yesterday's dinner, say) is kept until they change
 * it. Left on "today", the date follows the calendar, so opening the app the
 * next morning gives the new day rather than a stale one.
 */
public final class TravelDate {

    private TravelDate() { }

    /** Midnight (local time) of the day containing {@code millis}. */
    public static long midnight(long millis) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(millis);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }

    /** True when the chosen day is not today: it should be kept as is. */
    public static boolean isPinned(long chosen, long now) {
        return midnight(chosen) != midnight(now);
    }

    /** The date to show: the pinned day, or else today. */
    public static long resolve(long stored, boolean pinned, long now) {
        if (!pinned || stored <= 0) return midnight(now);
        return midnight(stored);
    }
}
