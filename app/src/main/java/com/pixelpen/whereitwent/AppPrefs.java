package com.pixelpen.whereitwent;

import android.content.Context;

public final class AppPrefs {

    private static final String PREFS_NAME = "settings";
    private static final String KEY_CURRENCY_SYMBOL = "currency_symbol";
    private static final String KEY_CURRENCY_CODE = "currency_code";   // v3.3

    private AppPrefs() {}

    // ---------- Currency ----------

    /**
     * Sets the currency by symbol only (e.g. from a backup). The code is then
     * worked out from the symbol, as for settings saved before v3.3.
     */
    public static void setCurrencySymbol(Context context, String symbol) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENCY_SYMBOL, symbol)
                .remove(KEY_CURRENCY_CODE)
                .apply();
    }

    /** v3.3: sets the currency by ISO code; the symbol follows from it. */
    public static void setCurrency(Context context, String code) {
        TripCurrency.Currency c = TripCurrency.byCode(code);
        if (c == null) return;
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_CURRENCY_CODE, c.code)
                .putString(KEY_CURRENCY_SYMBOL, c.symbol)
                .apply();
    }

    /**
     * v3.3: ISO code of the Settings currency. Settings saved before v3.3
     * have only a symbol, so the code is worked out from it ("$" -> USD).
     */
    // ---- v3.4: Accounts only ----
    private static final String KEY_ACCOUNTS_ONLY = "accounts_only";

    /** True when the app is set to track Accounts only (no Daily Living). */
    public static boolean isAccountsOnly(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_ACCOUNTS_ONLY, false);
    }

    public static void setAccountsOnly(Context context, boolean on) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit().putBoolean(KEY_ACCOUNTS_ONLY, on).apply();
    }

    public static String getCurrencyCode(Context context) {
        String code = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENCY_CODE, null);
        if (code != null && TripCurrency.byCode(code) != null) return code;
        return TripCurrency.codeForSymbol(getCurrencySymbol(context));
    }

    public static String getCurrencySymbol(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getString(KEY_CURRENCY_SYMBOL, "฿");
    }
}
