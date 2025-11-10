package com.pixelpen.whereitwent;

import android.content.Context;
import android.content.SharedPreferences;

public final class DateRangePrefs {
    private static final String PREFS_NAME = "where_it_went_prefs";
    private static final String KEY_MONTHS = "date_range_months";
    private static final int DEFAULT_MONTHS = 3;

    private DateRangePrefs() {}

    public static int getMonths(Context context) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return sp.getInt(KEY_MONTHS, DEFAULT_MONTHS);
    }

    public static void setMonths(Context context, int months) {
        SharedPreferences sp = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_MONTHS, months).apply();
    }
}
