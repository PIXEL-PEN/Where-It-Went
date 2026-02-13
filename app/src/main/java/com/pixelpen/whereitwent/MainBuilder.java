package com.pixelpen.whereitwent;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class MainBuilder {

    public static List<MainRow> build(Context ctx, boolean twelveMonthMode) {

        List<MainRow> rows = new ArrayList<>();

        // -------- RECENT (Months) --------
        List<MonthGroup> months =
                twelveMonthMode
                        ? MonthBuilder.buildLast12Months(ctx)
                        : MonthBuilder.buildLast3Months(ctx);

        for (MonthGroup mg : months) {
            rows.add(mg);
        }

        // -------- SUMMARIES SECTION --------
        rows.add(new RowSectionHeader("Summaries"));

        // ---- Daily Living 12-month total ----
        List<MonthGroup> last12 =
                MonthBuilder.buildLast12Months(ctx);

        double total = 0;

        for (MonthGroup mg : last12) {
            total += mg.monthTotal;
        }

        String formatted =
                CurrencyUtils.format(
                        total,
                        AppPrefs.getCurrencySymbol(ctx)
                );

        rows.add(new RowSummary(
                "Daily Living (12 months)",
                formatted
        ));

        return rows;
    }
}
