package com.pixelpen.whereitwent;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MonthBuilder {

    private static final SimpleDateFormat ISO_FMT =
            new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

    private static final SimpleDateFormat LABEL_FMT =
            new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);

    // Stores the last built month list for auto-expand
    public static List<MonthGroup> lastBuilt = new ArrayList<>();

    /**
     * Build MonthGroup list (header + last 12 months)
     */
    public static List<MonthGroup> buildLast12Months(Context ctx) {

        List<MonthGroup> list = new ArrayList<>();

        // SINGLE SOURCE OF TRUTH FOR CURRENCY
        String currencySymbol = AppPrefs.getCurrencySymbol(ctx);

        ExpenseDatabase db = ExpenseDatabase.getDatabase(ctx);
        List<Expense> all = db.expenseDao().getAll();

        Map<String, List<Expense>> map = new HashMap<>();
        double grandTotal = 0;

        for (Expense e : all) {
            String iso = DateUtils.toIso(e.date);
            if (iso == null) continue;

            String ym = iso.substring(0, 7);
            map.computeIfAbsent(ym, k -> new ArrayList<>()).add(e);

            grandTotal += e.amount;
        }

        // HEADER
        MonthGroup header = MonthGroup.makeHeader();
        header.monthLabel = "Last 12 Months";
        header.totalFormatted = CurrencyUtils.format(grandTotal, currencySymbol);
        list.add(header);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        for (int i = 0; i < 12; i++) {

            String key = ISO_FMT.format(cal.getTime());
            String label = LABEL_FMT.format(cal.getTime());

            MonthGroup group = new MonthGroup(label);
            group.expanded = false;
            group.isoMonth = key;

            List<Expense> monthList = map.get(key);
            double monthlyTotal = 0;

            if (monthList != null && !monthList.isEmpty()) {

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

                    String ui = DateUtils.isoToUi(dd.iso);
                    String stacked = DateUtils.toMonthStacked(ui);
                    String[] parts = stacked.split(" ");

                    if (parts.length == 2) {
                        dd.monthAbbrev = parts[0];
                        dd.dayNumber = parts[1];
                    } else {
                        dd.monthAbbrev = "";
                        dd.dayNumber = "";
                    }

                    dd.description = e.description;
                    dd.category = e.category.toUpperCase(Locale.ENGLISH);
                    dd.amount = CurrencyUtils.format(e.amount, currencySymbol);

                    group.dayRows.add(dd);
                }
            }

            group.monthTotal = monthlyTotal;
            group.totalFormatted = CurrencyUtils.format(monthlyTotal, currencySymbol);

            list.add(group);
            cal.add(Calendar.MONTH, -1);
        }

        lastBuilt = list;
        return list;
    }

    /**
     * Find index of a month (yyyy-MM) in the last-built month list.
     */
    public static int findMonthIndex(String monthKey) {
        if (monthKey == null) return -1;

        for (int i = 0; i < lastBuilt.size(); i++) {
            MonthGroup mg = lastBuilt.get(i);
            if (!mg.isHeader && monthKey.equals(mg.isoMonth)) {
                return i;
            }
        }

        return -1;
    }
}
