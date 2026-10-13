package com.pixelpen.whereitwent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

/**
 * 12-month category summary (v3.1).
 *
 * Totals per category for Daily Living and for Accounts over any
 * 12 consecutive calendar months. Item detail is intentionally left out.
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
    public final Map<String, Double> accounts;

    public final double dailyLivingTotal;
    public final double accountsTotal;

    private CategorySummary(String startYm, String endYm,
                            Map<String, Double> dailyLiving,
                            Map<String, Double> accounts) {
        this.startYm = startYm;
        this.endYm = endYm;
        this.dailyLiving = dailyLiving;
        this.accounts = accounts;
        this.dailyLivingTotal = sum(dailyLiving);
        this.accountsTotal = sum(accounts);
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
     * @param endYm        last month of the period, "yyyy-MM"
     */
    public static CategorySummary build(Collection<Expense> expenses,
                                        Collection<AccountItemEntity> accountItems,
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

        Map<String, Double> acc = newCategoryMap();
        if (accountItems != null) {
            for (AccountItemEntity it : accountItems) {
                String ym = monthOf(it.date, it.dateMillis);
                if (inRange(ym, startYm, endYm)) {
                    add(acc, it.category, it.amount);
                }
            }
        }

        return new CategorySummary(startYm, endYm, dl, acc);
    }

    /**
     * Months ("yyyy-MM") that can end a 12-month period: from {@code currentYm}
     * back to the earliest month holding any data, newest first. Always contains
     * at least {@code currentYm}.
     */
    public static List<String> availablePeriodEnds(Collection<Expense> expenses,
                                                   Collection<AccountItemEntity> accountItems,
                                                   String currentYm) {
        String earliest = currentYm;

        if (expenses != null) {
            for (Expense e : expenses) {
                String ym = monthOf(e.date, 0);
                if (ym != null && ym.compareTo(earliest) < 0) earliest = ym;
            }
        }
        if (accountItems != null) {
            for (AccountItemEntity it : accountItems) {
                String ym = monthOf(it.date, it.dateMillis);
                if (ym != null && ym.compareTo(earliest) < 0) earliest = ym;
            }
        }

        List<String> ends = new ArrayList<>();
        String ym = currentYm;
        while (ym.compareTo(earliest) >= 0) {
            ends.add(ym);
            ym = addMonths(ym, -1);
        }
        return ends;
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
    public String toCsv() {
        StringBuilder sb = new StringBuilder();
        sb.append("Category Summary,").append(csv(periodLabel())).append("\n");
        sb.append("Section,Category,Total\n");

        for (Map.Entry<String, Double> en : dailyLiving.entrySet()) {
            sb.append("Daily Living,").append(csv(en.getKey())).append(",")
                    .append(plain(en.getValue())).append("\n");
        }
        sb.append("Daily Living,TOTAL,").append(plain(dailyLivingTotal)).append("\n");

        for (Map.Entry<String, Double> en : accounts.entrySet()) {
            sb.append("Accounts,").append(csv(en.getKey())).append(",")
                    .append(plain(en.getValue())).append("\n");
        }
        sb.append("Accounts,TOTAL,").append(plain(accountsTotal)).append("\n");

        sb.append("All,GRAND TOTAL,").append(plain(grandTotal())).append("\n");
        return sb.toString();
    }

    /** Simple HTML table, matching the style of the existing email exports. */
    public String toHtml(String currencySymbol) {
        StringBuilder h = new StringBuilder();
        h.append("<html><head><meta charset='utf-8'></head><body>");
        h.append("<h2>Category Summary</h2>");
        h.append("<p>").append(esc(periodLabel())).append("</p>");

        section(h, "Daily Living", dailyLiving, dailyLivingTotal, currencySymbol);
        section(h, "Accounts", accounts, accountsTotal, currencySymbol);

        h.append("<h3>Grand total: ")
                .append(esc(CurrencyUtils.format(grandTotal(), currencySymbol)))
                .append("</h3>");
        h.append("</body></html>");
        return h.toString();
    }

    private static void section(StringBuilder h, String title, Map<String, Double> rows,
                                double total, String sym) {
        h.append("<h3>").append(esc(title)).append("</h3>");
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
        h.append("<tr><td><b>Total</b></td><td align='right'><b>")
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
