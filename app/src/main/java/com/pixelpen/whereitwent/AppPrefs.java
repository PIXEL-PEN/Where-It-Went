package com.pixelpen.whereitwent;

import android.content.Context;

public final class AppPrefs {

    private static final String PREFS_NAME = "settings";
    private static final String KEY_CURRENCY_SYMBOL = "currency_symbol";

    private AppPrefs() {}

    // ---------- Currency ----------

    public static void setCurrencySymbol(Context context, String symbol) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENCY_SYMBOL, symbol)
                .apply();
    }

    public static String getCurrencySymbol(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENCY_SYMBOL, "฿");
    }
}
