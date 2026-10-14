package com.pixelpen.whereitwent;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Travel mode state (v3.3), shared by the main screen and the travel screen:
 * on/off, the trip, and the last category and date used, so the app reopens
 * where it was left. Kept in the "travel_mode" preferences.
 */
public final class TravelMode {

    private static final String PREFS = "travel_mode";
    private static final String K_ACTIVE = "active";
    private static final String K_TRIP = "trip_id";
    private static final String K_DATE = "date_millis";
    private static final String K_DATE_PINNED = "date_pinned";
    private static final String K_CATEGORY = "category_";   // + trip id

    public interface OnTrip { void onTrip(AccountEntity trip); }

    private TravelMode() { }

    private static SharedPreferences p(Context c) {
        return c.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** The trip travel mode is on for, or null (also switches off a trip that is gone). */
    public static AccountEntity activeTrip(Context c) {
        if (!p(c).getBoolean(K_ACTIVE, false)) return null;
        AccountEntity a = ExpenseDatabase.getDatabase(c).accountDao()
                .getById(p(c).getLong(K_TRIP, -1));
        if (a == null || a.archived || !"travel".equals(a.type)) {
            end(c);
            return null;
        }
        return a;
    }

    public static void begin(Context c, AccountEntity trip) {
        p(c).edit().putBoolean(K_ACTIVE, true).putLong(K_TRIP, trip.id).apply();
    }

    public static void end(Context c) {
        p(c).edit().putBoolean(K_ACTIVE, false).apply();
    }

    public static String category(Context c, long tripId) {
        return p(c).getString(K_CATEGORY + tripId, null);
    }

    public static void setCategory(Context c, long tripId, String category) {
        p(c).edit().putString(K_CATEGORY + tripId, category).apply();
    }

    /** The entry date to show: a day picked by hand, or else today. */
    public static long date(Context c) {
        return TravelDate.resolve(p(c).getLong(K_DATE, 0),
                p(c).getBoolean(K_DATE_PINNED, false), System.currentTimeMillis());
    }

    public static void setDate(Context c, long millis) {
        long day = TravelDate.midnight(millis);
        p(c).edit()
                .putLong(K_DATE, day)
                .putBoolean(K_DATE_PINNED, TravelDate.isPinned(day, System.currentTimeMillis()))
                .apply();
    }

    /** Opens the travel screen. */
    public static void open(Activity from) {
        from.startActivity(new Intent(from, TravelActivity.class));
    }

    /** Asks which Travel account to use (or explains how to make one). */
    public static void pickTrip(Activity a, long currentId, OnTrip onTrip) {
        pickTrip(a, currentId, onTrip, null, null);
    }

    /**
     * Trip picker, with the trips of a journey listed together
     * ("SE Asia 2026 › Vietnam"). From the travel screen it also offers
     * "Next country…" and "Journey…" (null = not shown).
     */
    public static void pickTrip(Activity a, long currentId, OnTrip onTrip,
                                Runnable onNextCountry, Runnable onJourney) {
        List<AccountEntity> trips = new ArrayList<>();
        for (AccountEntity acc : ExpenseDatabase.getDatabase(a).accountDao().getActiveAccounts()) {
            if ("travel".equals(acc.type)) trips.add(acc);
        }
        if (trips.isEmpty()) {
            new AlertDialog.Builder(a)
                    .setTitle("Travel mode")
                    .setMessage("Travel mode works with a Travel account. Create one first: "
                            + "tap +, choose Accounts, and add a new account of type Travel.")
                    .setPositiveButton("OK", null)
                    .show();
            return;
        }
        // Journeys first (alphabetical), each with its countries; then single trips
        java.util.Collections.sort(trips, (x, y) -> {
            String jx = Journey.clean(x.journey), jy = Journey.clean(y.journey);
            if (jx == null && jy != null) return 1;
            if (jx != null && jy == null) return -1;
            if (jx != null) {
                int c = jx.compareToIgnoreCase(jy);
                if (c != 0) return c;
            }
            return x.name.compareToIgnoreCase(y.name);
        });
        String[] names = new String[trips.size()];
        int checked = -1;
        for (int i = 0; i < trips.size(); i++) {
            AccountEntity t = trips.get(i);
            String j = Journey.clean(t.journey);
            names[i] = (j != null ? j + "  ›  " : "") + t.name
                    + (t.hasForeignCurrency() ? "  (" + t.foreignCurrency + ")"
                    : t.homeCurrency != null && !t.homeCurrency.equalsIgnoreCase(
                            AppPrefs.getCurrencyCode(a)) ? "  (" + t.homeCurrency + ")" : "");
            if (t.id == currentId) checked = i;
        }
        AlertDialog.Builder b = new AlertDialog.Builder(a)
                .setTitle("Travel mode: which trip?")
                .setSingleChoiceItems(names, checked, (d, which) -> {
                    d.dismiss();
                    begin(a, trips.get(which));
                    onTrip.onTrip(trips.get(which));
                })
                .setNegativeButton("Cancel", null);
        if (onNextCountry != null) b.setNeutralButton("Next country…", (d, w) -> onNextCountry.run());
        if (onJourney != null) b.setPositiveButton("Journey…", (d, w) -> onJourney.run());
        b.show();
    }
}
