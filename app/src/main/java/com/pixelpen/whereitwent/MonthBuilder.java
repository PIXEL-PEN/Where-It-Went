package com.pixelpen.whereitwent;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

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

        MonthGroup header = MonthGroup.makeHeader();
        header.monthLabel = "Last 12 Months";
        header.totalFormatted = CurrencyUtils.format(grandTotal, currencySymbol);
        list.add(header);

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        LayoutInflater inflater = LayoutInflater.from(ctx);

        for (int i = 0; i < 12; i++) {

            String key = ISO_FMT.format(cal.getTime());
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
                    return ib.compareTo(ia); // newest first
                });

                for (Expense e : monthList) {
                    monthlyTotal += e.amount;

                    View row = inflater.inflate(R.layout.row_month_entry, null, false);

                    String ui = DateUtils.isoToUi(DateUtils.toIso(e.date));
                    String shortDate = DateUtils.toMonthStacked(ui);

                    TextView m = row.findViewById(R.id.text_month_abbrev);
                    TextView d = row.findViewById(R.id.text_day_number);
                    TextView item = row.findViewById(R.id.text_item);
                    TextView cat = row.findViewById(R.id.text_category);
                    TextView amt = row.findViewById(R.id.text_amount);

                    String[] parts = shortDate.split(" ");
                    if (parts.length == 2) {
                        m.setText(parts[0]);
                        d.setText(parts[1]);
                    }

                    item.setText(e.description);
                    cat.setText(e.category.toUpperCase(Locale.ENGLISH));
                    amt.setText(CurrencyUtils.format(e.amount, currencySymbol));

                    group.dayRows.add(row);
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
