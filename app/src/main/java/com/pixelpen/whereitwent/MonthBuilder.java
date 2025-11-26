package com.pixelpen.whereitwent;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.*;

public class MonthBuilder {

    private static final SimpleDateFormat ISO_MONTH_FMT =
            new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

    private static final SimpleDateFormat LABEL_FMT =
            new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);

    /**
     * Build MonthGroup list for the last 12 months.
     */
    public static List<MonthGroup> buildLast12Months(Context ctx, String currencySymbol) {

        List<MonthGroup> list = new ArrayList<>();

        ExpenseDatabase db = ExpenseDatabase.getDatabase(ctx);
        List<Expense> all = db.expenseDao().getAll();

        // Group by yyyy-MM → List<Expense>
        Map<String, List<Expense>> map = new HashMap<>();
        double grandTotal = 0;

        for (Expense e : all) {

            // Normalize every date to ISO yyyy-MM-dd
            String iso = DateUtils.toIso(e.date);
            if (iso == null) continue;

            String monthIso = iso.substring(0, 7);

            map.computeIfAbsent(monthIso, k -> new ArrayList<>()).add(e);
            grandTotal += e.amount;
        }

        // --------------------------------------------------------------------
        // HEADER: "Last 12 Months"
        // --------------------------------------------------------------------
        MonthGroup header = MonthGroup.makeHeader();
        header.monthLabel = "Last 12 Months";
        header.monthIso   = "";  // header has no month
        header.totalFormatted = CurrencyUtils.format(grandTotal, currencySymbol);

        list.add(header);

        // --------------------------------------------------------------------
        // Build 12 months descending (now → past)
        // --------------------------------------------------------------------
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        String currentMonthIso = ISO_MONTH_FMT.format(cal.getTime());

        for (int i = 0; i < 12; i++) {

            String monthIso = ISO_MONTH_FMT.format(cal.getTime());    // yyyy-MM
            String label    = LABEL_FMT.format(cal.getTime());        // "Nov 2025"

            MonthGroup group = new MonthGroup(label);
            group.monthIso = monthIso;

            List<Expense> monthList = map.get(monthIso);
            double monthlyTotal = 0;

            if (monthList != null && !monthList.isEmpty()) {

                // Sort newest first (ISO lexicographic sort works)
                monthList.sort((a, b) -> {
                    String ia = DateUtils.toIso(a.date);
                    String ib = DateUtils.toIso(b.date);
                    if (ia == null || ib == null) return 0;
                    return ib.compareTo(ia);
                });

                for (Expense e : monthList) {

                    monthlyTotal += e.amount;

                    MonthGroup.DayData dd = new MonthGroup.DayData();

                    dd.iso = DateUtils.toIso(e.date);

                    // Convert "yyyy-MM-dd" → "dd MMM yyyy" → stacked "Nov 24"
                    String uiDate    = DateUtils.isoToUi(dd.iso);
                    String stacked   = DateUtils.toMonthStacked(uiDate);

                    String[] parts = stacked.split(" ");
                    if (parts.length == 2) {
                        dd.monthAbbrev = parts[0];
                        dd.dayNumber   = parts[1];
                    } else {
                        dd.monthAbbrev = "";
                        dd.dayNumber   = "";
                    }

                    dd.description = e.description;
                    dd.category    = e.category.toUpperCase(Locale.ENGLISH);
                    dd.amount      = CurrencyUtils.format(e.amount, currencySymbol);

                    group.dayRows.add(dd);
                }
            }

            group.monthTotal = monthlyTotal;
            group.totalFormatted = CurrencyUtils.format(monthlyTotal, currencySymbol);

            // Auto-expand current month
            group.expanded = monthIso.equals(currentMonthIso);

            list.add(group);

            cal.add(Calendar.MONTH, -1);
        }

        return list;
    }
}
