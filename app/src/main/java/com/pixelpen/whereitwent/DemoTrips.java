package com.pixelpen.whereitwent;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * v3.3 prototype only: two demo Travel accounts with a trip currency, so the
 * foreign-currency screens can be looked at straight away. Offered in the
 * Accounts menu of debug builds (WIW-dev) only. Rates are illustrative.
 *
 *  - Vietnam trip (demo): dong, home currency = the Settings currency
 *    (baht on your phone), rate changed partway through the trip.
 *  - China trip (demo): yuan, home currency US dollars, so it shows as a
 *    separate currency in the Yearly Summary.
 */
public final class DemoTrips {

    public static final String MENU_TITLE = "Add demo trips (dev)";

    private DemoTrips() { }

    /** @return number of demo trips added (0 if they already exist). */
    public static int add(Context ctx) {
        ExpenseDatabase db = ExpenseDatabase.getDatabase(ctx);
        String settingsCode = AppPrefs.getCurrencyCode(ctx);
        int[] added = {0};

        db.runInTransaction(() -> {
            // Vietnam: 1 home = 780 dong, later 795 (rates for 1 THB; scaled if home differs)
            double vnFirst = "THB".equals(settingsCode) ? 780 : 25_450;
            double vnLater = "THB".equals(settingsCode) ? 795 : 25_900;
            Long vn = create(db, "Vietnam trip (demo)", "VND", settingsCode, vnLater);
            if (vn != null) {
                item(db, vn, 2026, Calendar.SEPTEMBER, 18, "Hotel, Hanoi", "Lodging", 2_340_000, vnFirst, settingsCode);
                item(db, vn, 2026, Calendar.SEPTEMBER, 19, "Pho and coffee", "Meals", 185_000, vnFirst, settingsCode);
                item(db, vn, 2026, Calendar.SEPTEMBER, 20, "Train to Hue", "Transportation", 1_120_000, vnFirst, settingsCode);
                item(db, vn, 2026, Calendar.SEPTEMBER, 22, "Ha Long Bay cruise", "Activities", 3_900_000, vnFirst, settingsCode);
                item(db, vn, 2026, Calendar.OCTOBER, 3, "Street food tour", "Meals", 650_000, vnLater, settingsCode);
                item(db, vn, 2026, Calendar.OCTOBER, 4, "Grab rides", "Transportation", 240_000, vnLater, settingsCode);
                added[0]++;
            }

            // China: 1 USD = 7.10 yuan, later 7.18
            Long cn = create(db, "China trip (demo)", "CNY", "USD", 7.18);
            if (cn != null) {
                item(db, cn, 2026, Calendar.AUGUST, 10, "Hotel, Shanghai", "Lodging", 1_280, 7.10, "USD");
                item(db, cn, 2026, Calendar.AUGUST, 11, "Dumplings", "Meals", 96.50, 7.10, "USD");
                item(db, cn, 2026, Calendar.AUGUST, 12, "High-speed train to Beijing", "Transportation", 553, 7.10, "USD");
                item(db, cn, 2026, Calendar.AUGUST, 14, "Forbidden City tickets", "Activities", 120, 7.18, "USD");
                added[0]++;
            }
        });
        return added[0];
    }

    /** Creates the Travel account with its currency; null if the name is taken. */
    private static Long create(ExpenseDatabase db, String name, String foreign, String home, double rate) {
        AccountDao dao = db.accountDao();
        if (dao.getAccountByName(name) != null) return null;
        dao.insert(new AccountEntity(name, "travel", false));
        AccountEntity a = dao.getAccountByName(name);
        dao.setCurrency(a.id, foreign, home, rate);
        return a.id;
    }

    private static void item(ExpenseDatabase db, long accountId, int y, int m, int d,
                             String what, String category, double foreign, double rate, String home) {
        Calendar c = Calendar.getInstance();
        c.clear();
        c.set(y, m, d);   // local midnight, as entries made in the app
        String date = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH).format(c.getTime());
        AccountItemEntity it = new AccountItemEntity(accountId, date, c.getTimeInMillis(),
                what, category, TripCurrency.toHome(foreign, rate, home), null);
        it.foreignAmount = foreign;
        it.rate = rate;
        db.accountItemDao().insert(it);
    }
}
