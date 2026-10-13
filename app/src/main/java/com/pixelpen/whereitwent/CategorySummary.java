package com.pixelpen.whereitwent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 12-month category summary (v3.1).
 *
 * Totals per category for Daily Living, and per account then per category
 * for Accounts, over 12 consecutive calendar months. Item detail is left out.
 *
 * Pure Java (no Android imports) so the period and total logic can be
 * unit-tested off-device. Rendering to CSV / HTML lives here too.
 */
public final class CategorySummary {

    /** Number of months in a summary period. */
    public static final int PERIOD_MONTHS = 12;

    // Period bounds as "yyyy-MM", inclusive
    public final String startYm;
    public final String endYm;

    // category -> total, sorted by category name (case-insensitive)
    public final Map<String, Double> dailyLiving;

    // Accounts with items in this period: active first, then archived, each by name
    public final List<AccountGroup> accounts;

    public final double dailyLivingTotal;
    public final double accountsTotal;

    /** One account's category totals within the period. */
    public static final class AccountGroup {
        public final long accountId;
        public final String name;
        public final boolean archived;
        public final Map<String, Double> categories = newCategoryMap();
        private double total;

        AccountGroup(long accountId, String name, boolean archived) {
            this.accountId = accountId;
            this.name = name;
            this.archived = archived;
        }

        public double total() {
            return total;
        }

        /** Name as shown and exported, e.g. "Condo (archived)". */
        public String displayName() {
            return archived ? name + " (archived)" : name;
        }
    }

    private CategorySummary(String startYm, String endYm,
                            Map<String, Double> dailyLiving,
                            List<AccountGroup> accounts) {
        this.startYm = startYm;
        this.endYm = endYm;
        this.dailyLiving = dailyLiving;
        this.accounts = accounts;
        this.dailyLivingTotal = sum(dailyLiving);
        double t = 0;
        for (AccountGroup g : accounts) t += g.total;
        this.accountsTotal = t;
    }

    public double grandTotal() {
        return dailyLivingTotal + accountsTotal;
    }

    // --------------------------------------------------------
    // BUILD
    // --------------------------------------------------------

    /**
     * Build the summary for the 12 months ending with {@code endYm}
     * (e.g. "2026-10" covers Nov 2025 – Oct 2026).
     *
     * @param expenses     Daily Living expenses (any date; out-of-period rows are ignored)
     * @param accountItems Account items from all accounts, active and archived
     * @param accountList  all accounts, used for names and archived state
     * @param endYm        last month of the period, "yyyy-MM"
     */
    public static CategorySummary build(Collection<Expense> expenses,
                                        Collection<AccountItemEntity> accountItems,
                                        Collection<AccountEntity> accountList,
                                        String endYm) {

        String startYm = addMonths(endYm, -(PERIOD_MONTHS - 1));

        Map<String, Double> dl = newCategoryMap();
        if (expenses != null) {
            for (Expense e : expenses) {
                String ym = monthOf(e.date, 0);
                if (inRange(ym, startYm, endYm)) {
                    add(dl, e.category, e.amount);
                }
            }
        }

        Map<Long, AccountEntity> byId = new HashMap<>();
        if (accountList != null) {
            for (AccountEntity a : accountList) byId.put(a.id, a);
        }

        Map<Long, AccountGroup> groups = new HashMap<>();
        if (accountItems != null) {
            for (AccountItemEntity it : accountItems) {
                String ym = monthOf(it.date, it.dateMillis);
                if (!inRange(ym, startYm, endYm)) continue;

                AccountGroup g = groups.get(it.accountId);
                if (g == null) {
                    AccountEntity a = byId.get(it.accountId);
                    g = (a != null)
                            ? new AccountGroup(a.id, a.name == null ? "Account" : a.name, a.archived)
                            : new AccountGroup(it.accountId, "Unknown account", false);
                    groups.put(it.accountId, g);
                }
                add(g.categories, it.category, it.amount);
                g.total += it.amount;
            }
        }

        List<AccountGroup> acc = new ArrayList<>(groups.values());
        acc.sort((x, y) -> {
            if (x.archived != y.archived) return x.archived ? 1 : -1;
            int c = String.CASE_INSENSITIVE_ORDER.compare(x.name, y.name);
            return c != 0 ? c : Long.compare(x.accountId, y.accountId);
        });

        return new CategorySummary(startYm, endYm, dl, acc);
    }

    /**
     * Month ("yyyy-MM") of the earliest record in either Daily Living or
     * Accounts, or null when there is no data at all.
     */
    public static String earliestMonth(Collection<Expense> expenses,
                                       Collection<AccountItemEntity> accountItems) {
        String earliest = null;
        if (expenses != null) {
            for (Expense e : expenses) {
                String ym = monthOf(e.date, 0);
                if (ym != null && (earliest == null || ym.compareTo(earliest) < 0)) earliest = ym;
            }
        }
        if (accountItems != null) {
            for (AccountItemEntity it : accountItems) {
                String ym = monthOf(it.date, it.dateMillis);
                if (ym != null && (earliest == null || ym.compareTo(earliest) < 0)) earliest = ym;
            }
        }
        return earliest;
    }

    /**
     * Summary periods anchored to the user's start month (the month of their
     * earliest record), newest first, as the "yyyy-MM" month each period ends.
     *
     * Someone who started in Jan 2026 gets Jan–Dec periods; someone who started
     * in May 2026 gets May–Apr periods. The last period contains {@code currentYm}
     * and is usually still in progress. With no data, the single period starts
     * in the current month.
     */
    public static List<String> anchoredPeriodEnds(Collection<Expense> expenses,
                                                  Collection<AccountItemEntity> accountItems,
                                                  String currentYm) {
        String start = earliestMonth(expenses, accountItems);
        if (start == null || start.compareTo(currentYm) > 0) start = currentYm;

        List<String> ends = new ArrayList<>();
        String periodStart = start;
        while (periodStart.compareTo(currentYm) <= 0) {
            ends.add(0, addMonths(periodStart, PERIOD_MONTHS - 1));
            periodStart = addMonths(periodStart, PERIOD_MONTHS);
        }
        return ends;
    }

    /** True while {@code currentYm} falls inside this period (period not yet complete). */
    public boolean isInProgress(String currentYm) {
        // The final month counts as in progress until it is over
        return currentYm.compareTo(startYm) >= 0 && currentYm.compareTo(endYm) <= 0;
    }

    /** Months of this period that have started by {@code currentYm}, 0–12. */
    public int monthsElapsed(String currentYm) {
        int n = monthsBetween(startYm, currentYm) + 1;
        return Math.max(0, Math.min(PERIOD_MONTHS, n));
    }

    /** "In progress · 10 of 12 months" or "Complete". */
    public String statusLabel(String currentYm) {
        if (isInProgress(currentYm)) {
            return "In progress · " + monthsElapsed(currentYm) + " of " + PERIOD_MONTHS + " months";
        }
        return "Complete";
    }

    /** Whole months from {@code fromYm} to {@code toYm} (negative if earlier). */
    static int monthsBetween(String fromYm, String toYm) {
        int fy = Integer.parseInt(fromYm.substring(0, 4));
        int fm = Integer.parseInt(fromYm.substring(5, 7));
        int ty = Integer.parseInt(toYm.substring(0, 4));
        int tm = Integer.parseInt(toYm.substring(5, 7));
        return (ty - fy) * 12 + (tm - fm);
    }

    // --------------------------------------------------------
    // RENDER
    // --------------------------------------------------------

    /** "Nov 2025 – Oct 2026" */
    public String periodLabel() {
        return periodLabel(endYm);
    }

    public static String periodLabel(String endYm) {
        return monthLabel(addMonths(endYm, -(PERIOD_MONTHS - 1)))
                + " – " + monthLabel(endYm);
    }

    /** "Nov2025-Oct2026", safe for file names. */
    public String fileTag() {
        return monthLabel(startYm).replace(" ", "")
                + "-" + monthLabel(endYm).replace(" ", "");
    }

    /** CSV with plain two-decimal amounts (no currency symbol) for spreadsheets. */
    public String toCsv(String currentYm) {
        StringBuilder sb = new StringBuilder();
        sb.append("Category Summary,").append(csv(periodLabel())).append(",")
                .append(csv(statusLabel(currentYm))).append("\n");
        sb.append("Section,Account,Category,Total\n");

        for (Map.Entry<String, Double> en : dailyLiving.entrySet()) {
            sb.append("Daily Living,,").append(csv(en.getKey())).append(",")
                    .append(plain(en.getValue())).append("\n");
        }
        sb.append("Daily Living,,TOTAL,").append(plain(dailyLivingTotal)).append("\n");

        for (AccountGroup g : accounts) {
            String name = csv(g.displayName());
            for (Map.Entry<String, Double> en : g.categories.entrySet()) {
                sb.append("Accounts,").append(name).append(",").append(csv(en.getKey()))
                        .append(",").append(plain(en.getValue())).append("\n");
            }
            sb.append("Accounts,").append(name).append(",SUBTOTAL,")
                    .append(plain(g.total)).append("\n");
        }
        sb.append("Accounts,,TOTAL,").append(plain(accountsTotal)).append("\n");

        sb.append("All,,GRAND TOTAL,").append(plain(grandTotal())).append("\n");
        return sb.toString();
    }

    /** Simple HTML table, matching the style of the existing email exports. */
    public String toHtml(String currencySymbol, String currentYm) {
        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='utf-8'></head><body>");
        h.append("<h2>Category Summary</h2>");
        h.append("<p>").append(esc(periodLabel())).append("<br><i>")
                .append(esc(statusLabel(currentYm))).append("</i></p>");

        h.append("<h3>Daily Living</h3>");
        table(h, dailyLiving, "Total", dailyLivingTotal, currencySymbol);

        h.append("<h3>Accounts: ")
                .append(esc(CurrencyUtils.format(accountsTotal, currencySymbol)))
                .append("</h3>");
        if (accounts.isEmpty()) {
            h.append("<p><i>No records in this period</i></p>");
        }
        for (AccountGroup g : accounts) {
            h.append("<h4>").append(esc(g.displayName())).append("</h4>");
            table(h, g.categories, "Subtotal", g.total, currencySymbol);
        }

        h.append("<h3>Grand total: ")
                .append(esc(CurrencyUtils.format(grandTotal(), currencySymbol)))
                .append("</h3>");
        h.append("</body></html>");
        return h.toString();
    }

    private static void table(StringBuilder h, Map<String, Double> rows,
                              String totalLabel, double total, String sym) {
        h.append("<table border='1' cellpadding='4' cellspacing='0'>");
        h.append("<tr><th>Category</th><th>Total</th></tr>");
        if (rows.isEmpty()) {
            h.append("<tr><td colspan='2'><i>No records in this period</i></td></tr>");
        }
        for (Map.Entry<String, Double> en : rows.entrySet()) {
            h.append("<tr><td>").append(esc(en.getKey())).append("</td>")
                    .append("<td align='right'>")
                    .append(esc(CurrencyUtils.format(en.getValue(), sym)))
                    .append("</td></tr>");
        }
        h.append("<tr><td><b>").append(totalLabel).append("</b></td><td align='right'><b>")
                .append(esc(CurrencyUtils.format(total, sym)))
                .append("</b></td></tr>");
        h.append("</table><br>");
    }

    // --------------------------------------------------------
    // HELPERS
    // --------------------------------------------------------

    /** Month key "yyyy-MM" for a stored date string, falling back to epoch millis. */
    static String monthOf(String rawDate, long millis) {
        String iso = DateUtils.toIso(rawDate);
        if (iso != null && iso.length() >= 7) return iso.substring(0, 7);
        if (millis > 0) {
            return new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).format(new Date(millis));
        }
        return null;
    }

    static boolean inRange(String ym, String startYm, String endYm) {
        return ym != null && ym.compareTo(startYm) >= 0 && ym.compareTo(endYm) <= 0;
    }

    /** Shift a "yyyy-MM" key by n months. */
    public static String addMonths(String ym, int n) {
        int y = Integer.parseInt(ym.substring(0, 4));
        int m = Integer.parseInt(ym.substring(5, 7)) - 1 + n;
        y += Math.floorDiv(m, 12);
        m = Math.floorMod(m, 12);
        return String.format(Locale.ENGLISH, "%04d-%02d", y, m + 1);
    }

    /** Current month as "yyyy-MM". */
    public static String currentYm() {
        return new SimpleDateFormat("yyyy-MM", Locale.ENGLISH)
                .format(Calendar.getInstance().getTime());
    }

    static String monthLabel(String ym) {
        try {
            Date d = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH).parse(ym);
            return new SimpleDateFormat("MMM yyyy", Locale.ENGLISH).format(d);
        } catch (Exception e) {
            return ym;
        }
    }

    private static Map<String, Double> newCategoryMap() {
        return new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    }

    private static void add(Map<String, Double> map, String category, double amount) {
        String key = (category == null || category.trim().isEmpty())
                ? "Uncategorised" : category.trim();
        Double cur = map.get(key);
        map.put(key, (cur == null ? 0 : cur) + amount);
    }

    private static double sum(Map<String, Double> map) {
        double t = 0;
        for (double v : map.values()) t += v;
        return t;
    }

    private static String plain(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static String csv(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}
