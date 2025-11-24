package com.pixelpen.whereitwent;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.*;

public class MonthBuilder {

    // Date formats
    private static final SimpleDateFormat ISO_FMT =
            new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);

    private static final SimpleDateFormat LABEL_FMT =
            new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);

    // --------------------------------------------------------
    // MAIN ENTRY
    // --------------------------------------------------------
    public static List<MonthGroup> buildLast12Months(Context ctx) {

        List<MonthGroup> list = new ArrayList<>();

        // Add the header row
        MonthGroup header = MonthGroup.makeHeader();
        header.monthLabel = "Last 12 Months";
        list.add(header);

        // Determine month range
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        Calendar end = (Calendar) cal.clone();

        Calendar start = (Calendar) cal.clone();
        start.add(Calendar.MONTH, -11);   // past 12 months total

        // Get expenses from DB
        ExpenseDatabase db = ExpenseDatabase.getDatabase(ctx);
        List<Expense> all = db.expenseDao().getAll();

        // Preprocess expenses into buckets "yyyy-MM" → list
        Map<String, List<Expense>> map = new HashMap<>();

        for (Expense e : all) {
            String iso = DateUtils.toIso(e.date);
            if (iso == null) continue;

            String ym = iso.substring(0, 7);   // yyyy-MM

            if (!map.containsKey(ym))
                map.put(ym, new ArrayList<>());

            map.get(ym).add(e);
        }

        // Build each month in descending order (latest first)
        Calendar walker = (Calendar) end.clone();
        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (int i = 0; i < 12; i++) {

            String key = ISO_FMT.format(walker.getTime());      // yyyy-MM
            String label = LABEL_FMT.format(walker.getTime());  // "Nov 2025"

            MonthGroup group = new MonthGroup(label);
            group.expanded = false;  // collapsed by default

            List<Expense> monthList = map.get(key);

            // --------------------------------------------
            // BUILD ROWS FOR THIS MONTH
            // --------------------------------------------
            double total = 0.0;

            if (monthList != null && !monthList.isEmpty()) {

                // Sort newest first
                Collections.sort(monthList, (a, b) -> {
                    String ia = DateUtils.toIso(a.date);
                    String ib = DateUtils.toIso(b.date);
                    if (ia == null || ib == null) return 0;
                    return ib.compareTo(ia); // DESC
                });

                for (Expense e : monthList) {
                    View row = inflater.inflate(R.layout.row_month_entry, null, false);

                    // Stacked date
                    String ui = DateUtils.isoToUi(DateUtils.toIso(e.date));
                    String shortDate = DateUtils.toMonthStacked(ui);

                    TextView m = row.findViewById(R.id.text_month_abbrev);
                    TextView d = row.findViewById(R.id.text_day_number);
                    TextView item = row.findViewById(R.id.text_item);
                    TextView cat = row.findViewById(R.id.text_category);
                    TextView amt = row.findViewById(R.id.text_amount);

                    String[] parts = shortDate.split(" ");
                    if (parts.length == 2) {
                        m.setText(parts[0]); // "Nov"
                        d.setText(parts[1]); // "02"
                    }

                    item.setText(e.description);
                    cat.setText(e.category.toUpperCase(Locale.ENGLISH));
                    amt.setText(String.format(Locale.ENGLISH, "%.2f", e.amount));

                    total += e.amount;
                    group.dayRows.add(row);
                }
            }

            // Store monthly total
            group.total = String.format(Locale.ENGLISH, "%.2f ₽", total);

            list.add(group);
            walker.add(Calendar.MONTH, -1);
        }

        // --------------------------------------------------------
        // COMPUTE GRAND TOTAL FOR HEADER
        // --------------------------------------------------------
        double grand = 0.0;

        for (MonthGroup g : list) {
            if (!g.isHeader && g.dayRows.size() > 0) {
                try {
                    grand += Double.parseDouble(
                            g.total.replace("₱", "").trim()
                    );
                } catch (Exception ignore) {}
            }
        }

        header.total = String.format(Locale.ENGLISH, "%.2f ₽", grand);

        return list;
    }
}
