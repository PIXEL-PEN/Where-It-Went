package com.pixelpen.whereitwent;

import android.content.Context;

import java.util.ArrayList;
import java.util.List;

public class MainBuilder {

    public static List<MainRow> build(
            Context ctx,
            boolean twelveMonthMode,
            boolean summariesExpanded
    ) {

        List<MainRow> rows = new ArrayList<>();

        // -----------------------------
        // RECENT (Months Section)
        // -----------------------------
        List<MonthGroup> months =
                twelveMonthMode
                        ? MonthBuilder.buildLast12Months(ctx)
                        : MonthBuilder.buildLast3Months(ctx);

        for (MonthGroup mg : months) {
            rows.add(mg);
        }

        // -----------------------------
// SUMMARIES HEADER
// -----------------------------
        rows.add(new RowSummaryHeader(summariesExpanded));

// -----------------------------
// SUMMARY ROWS
// -----------------------------
        if (summariesExpanded) {

            // -----------------------------
            // DAILY LIVING (12 months)
            // -----------------------------
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

            // -----------------------------
            // ACCOUNTS (current)
            // -----------------------------
            rows.add(new RowSummary(
                    "Accounts (current)",
                    ""
            ));

            ExpenseDatabase db =
                    ExpenseDatabase.getDatabase(ctx);

            List<AccountEntity> accounts =
                    db.accountDao().getAllAccounts();

            for (AccountEntity acct : accounts) {

                double acctTotal = 0;

                List<AccountItemEntity> items =
                        db.accountItemDao()
                                .getItemsForAccount(acct.id);

                for (AccountItemEntity item : items) {
                    acctTotal += item.amount;
                }

                String acctFormatted =
                        CurrencyUtils.format(
                                acctTotal,
                                AppPrefs.getCurrencySymbol(ctx)
                        );

                String label = acct.name;

                if (acct.archived) {
                    label += " (archived)";
                }

                rows.add(new RowSummary(
                        label,
                        acctFormatted
                ));

            }
        }

        return rows;
    }
}