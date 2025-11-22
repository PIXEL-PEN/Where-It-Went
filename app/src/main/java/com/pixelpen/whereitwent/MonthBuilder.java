package com.pixelpen.whereitwent;

import android.content.Context;

import java.text.SimpleDateFormat;
import java.util.*;

public class MonthBuilder {

    public static List<MonthGroup> buildLast12Months(Context ctx) {

        ExpenseDao dao = ExpenseDatabase.getDatabase(ctx).expenseDao();
        List<Expense> all = dao.getAll();  // newest → oldest by id desc

        List<MonthGroup> result = new ArrayList<>();

        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.DAY_OF_MONTH, 1);

        SimpleDateFormat isoMonthFmt = new SimpleDateFormat("yyyy-MM", Locale.ENGLISH);
        SimpleDateFormat labelFmt = new SimpleDateFormat("MMM yyyy", Locale.ENGLISH);

        Map<String, MonthGroup> map = new HashMap<>();

        for (int i = 0; i < 12; i++) {

            String isoMonth = isoMonthFmt.format(cal.getTime());
            String label = labelFmt.format(cal.getTime());

            MonthGroup group = new MonthGroup();
            group.isoMonth = isoMonth;
            group.monthLabel = label;
            result.add(group);
            map.put(isoMonth, group);

            cal.add(Calendar.MONTH, -1);
        }

        for (Expense e : all) {
            String iso = DateUtils.toIso(e.date);
            if (iso == null || iso.length() < 7) continue;

            String month = iso.substring(0, 7);
            MonthGroup mg = map.get(month);
            if (mg != null) {
                mg.items.add(e);
                mg.total += e.amount;
            }
        }

        return result;
    }
}
