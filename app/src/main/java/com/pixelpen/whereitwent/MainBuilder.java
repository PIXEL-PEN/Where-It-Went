package com.pixelpen.whereitwent;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class MainBuilder {

    public static List<MainRow> build(Context ctx, boolean twelveMonthMode) {

        List<MainRow> rows = new ArrayList<>();

        List<MonthGroup> months =
                twelveMonthMode
                        ? MonthBuilder.buildLast12Months(ctx)
                        : MonthBuilder.buildLast3Months(ctx);

        for (MonthGroup mg : months) {
            rows.add(mg);
        }

        return rows;
    }
}
