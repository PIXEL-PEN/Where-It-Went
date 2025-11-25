package com.pixelpen.whereitwent;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.*;

public class MonthBuilder {

    private static final SimpleDateFormat ISO_FMT =
            new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

    private static final SimpleDateFormat LABEL_FMT =
            new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);

    public static List<MonthGroup> buildLast12Months(Context ctx, String currencySymbol) {

        List<MonthGroup> list = new ArrayList<>();

        ExpenseDatabase db = ExpenseDatabase.getDatabase(ctx);
        List<Expense> all = db.expenseDao().getAll();

        Map<String, List<Expense>> map = new HashMap<>();
        double grandTotal = 0;

        // Group by yyyy-MM
        for (Expense e : all) {
            String iso = DateUtils.toIso(e.date);
            if (iso == null) continue;

            String ym = iso.substring(0, 7);

            if (!map.containsKey(ym))
                map.put(ym, new ArrayList<>());

            map.get(ym).add(e);

            grandTotal += e.amount;
        }

        // HEADER
        MonthGroup header = MonthGroup.makeHeader();
        header.monthLabel = "Last 12 Months";
        header.totalFormatted = CurrencyUtils.format(grandTotal, currencySymbol);
        list.add(header);

        // Build last 12 months (descending)
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        for (int i = 0; i < 12; i++) {

            String key   = ISO_FMT.format(cal.getTime());
            String label = LABEL_FMT.format(cal.getTime());

            MonthGroup group = new MonthGroup(label);
            group.expanded = false;

            List<Expense> monthList = map.get(key);
            double monthlyTotal = 0;

            if (monthList != null && !monthList.isEmpty()) {

                Collections.sort(monthList, (a, b) -> {
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
                    String[] parts = DateUtils.toMonthStacked(ui).split(" ");

                    dd.monthAbbrev = parts[0];
                    dd.dayNumber   = parts[1];
                    dd.description = e.description;
                    dd.category    = e.category.toUpperCase(Locale.ENGLISH);
                    dd.amount      = CurrencyUtils.format(e.amount, currencySymbol);

                    group.dayRows.add(dd);
                }
            }

            group.monthTotal = monthlyTotal;
            group.totalFormatted = CurrencyUtils.format(monthlyTotal, currencySymbol);

            list.add(group);
            cal.add(Calendar.MONTH, -1);
        }

        return list;
    }
}
