package com.pixelpen.whereitwent;

import android.content.Context;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Temporary pass-through to neutralize the old global Date Range Display.
 * Keeps the same API so existing calls compile, but returns all items unchanged.
 */
public final class DateRangeCutoff {

    private DateRangeCutoff() { }

    /** Returns a copy of items with no filtering applied. */
    public static List<Expense> filterByMonths(Context ctx, List<Expense> items) {
        if (items == null) return Collections.emptyList();
        return new ArrayList<>(items);
    }

    /** Indicates the global cutoff is disabled. */
    public static boolean isEnabled(Context ctx) {
        return false;
    }

    /** Placeholder: “no cutoff”. */
    public static int getActiveMonths(Context ctx) {
        return Integer.MAX_VALUE;
    }
}
