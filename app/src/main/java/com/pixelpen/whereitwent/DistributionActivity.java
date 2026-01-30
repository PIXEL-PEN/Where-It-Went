package com.pixelpen.whereitwent;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DistributionActivity extends AppCompatActivity {

    private static final String TAG_FIXED         = "Fixed";
    private static final String TAG_BASIC         = "Necessity";
    private static final String TAG_NECESSITIES   = "Necessities";
    private static final String TAG_DISCRETIONARY = "Discretionary";
    private static final String TAG_OTHER         = "Other";
    private static final String TAG_OFF_BUDGET    = "Off-Budget";

    private static final String[] DATE_PATTERNS = new String[]{
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "dd MMM yyyy",
            "dd MMM. yyyy",
            "d MMM yyyy",
            "d MMM. yyyy",
            "d MMMM yyyy"
    };

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_distribution);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainScreen.class));
                finish();
            });
        }

        refreshCurrencyValues();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshCurrencyValues();
    }

    private void refreshCurrencyValues() {

        // ✅ SINGLE SOURCE OF TRUTH
        String currencySymbol = AppPrefs.getCurrencySymbol(this);
        DecimalFormat money = new DecimalFormat("#,##0.00");

        SimplePieView pie = findViewById(R.id.pie);

        TextView fixedTotal = findViewById(R.id.legend_fixed_total);
        TextView fixedDelta = findViewById(R.id.legend_fixed_delta);
        TextView basicTotal = findViewById(R.id.legend_basic_total);
        TextView basicDelta = findViewById(R.id.legend_basic_delta);
        TextView discTotal  = findViewById(R.id.legend_disc_total);
        TextView discDelta  = findViewById(R.id.legend_disc_delta);

        List<Expense> all =
                ExpenseDatabase.getDatabase(this)
                        .expenseDao().getAll();

        Map<String, Map<String, Double>> byTagMonth =
                buildTagMonthTotals(all);

        List<String> months =
                collectSortedMonthsExcludingOffBudget(byTagMonth);

        if (months.isEmpty()) {
            pie.setValues(0f, 0f, 0f);
            setSafeLegend(fixedTotal, money.format(0) + " " + currencySymbol);
            setSafeLegend(basicTotal, money.format(0) + " " + currencySymbol);
            setSafeLegend(discTotal,  money.format(0) + " " + currencySymbol);
            setSafeLegend(fixedDelta, "—");
            setSafeLegend(basicDelta, "—");
            setSafeLegend(discDelta,  "—");
            return;
        }

        String latestMonth = months.get(months.size() - 1);
        String prevMonth =
                (months.size() >= 2)
                        ? months.get(months.size() - 2)
                        : null;

        double fixedLatest =
                sumBucketForMonth(byTagMonth, latestMonth, Bucket.FIXED);
        double basicLatest =
                sumBucketForMonth(byTagMonth, latestMonth, Bucket.BASIC);
        double discLatest =
                sumBucketForMonth(byTagMonth, latestMonth, Bucket.DISCRETIONARY);

        pie.setValues(
                (float) fixedLatest,
                (float) discLatest,
                (float) basicLatest
        );

        setSafeLegend(fixedTotal, money.format(fixedLatest) + " " + currencySymbol);
        setSafeLegend(basicTotal, money.format(basicLatest) + " " + currencySymbol);
        setSafeLegend(discTotal,  money.format(discLatest)  + " " + currencySymbol);

        if (prevMonth == null) {
            setSafeLegend(fixedDelta, "—");
            setSafeLegend(basicDelta, "—");
            setSafeLegend(discDelta,  "—");
        } else {
            double fixedPrev =
                    sumBucketForMonth(byTagMonth, prevMonth, Bucket.FIXED);
            double basicPrev =
                    sumBucketForMonth(byTagMonth, prevMonth, Bucket.BASIC);
            double discPrev =
                    sumBucketForMonth(byTagMonth, prevMonth, Bucket.DISCRETIONARY);

            setSafeLegend(fixedDelta, formatDeltaPct(fixedLatest, fixedPrev));
            setSafeLegend(basicDelta, formatDeltaPct(basicLatest, basicPrev));
            setSafeLegend(discDelta,  formatDeltaPct(discLatest,  discPrev));
        }

        renderOffBudget(all, money, currencySymbol);
    }

    /* -------------------------------------------------
       OFF-BUDGET CATEGORY SUMMARY (ALL-TIME)
       ------------------------------------------------- */
    private void renderOffBudget(
            List<Expense> all,
            DecimalFormat money,
            String currencySymbol
    ) {

        LinearLayout offBudgetContainer =
                findViewById(R.id.off_budget_container);
        if (offBudgetContainer == null) return;

        offBudgetContainer.removeAllViews();
        offBudgetContainer.setPadding(0, dp(20), 0, 0);

        View divider = new View(this);
        divider.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
        ));
        divider.setBackgroundColor(0x33000000);
        offBudgetContainer.addView(divider);

        TextView header = new TextView(this);
        header.setText("Off-Budget");
        header.setTextSize(15);
        header.setTypeface(Typeface.DEFAULT_BOLD);
        header.setTextColor(0xFF222222);
        header.setPadding(0, dp(8), 0, dp(4));
        offBudgetContainer.addView(header);

        Map<String, Double> totals = new LinkedHashMap<>();

        for (Expense e : all) {
            if (e.category == null) continue;
            String cat = e.category.trim();
            if (cat.isEmpty()) continue;

            String tag =
                    CategoryManager.getTagForCategory(this, cat);
            if (!TAG_OFF_BUDGET.equalsIgnoreCase(tag)) continue;

            totals.put(cat, totals.getOrDefault(cat, 0.0) + e.amount);
        }

        if (totals.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No Off-Budget categories");
            tv.setTextSize(14);
            tv.setTextColor(0xFF777777);
            tv.setPadding(0, dp(4), 0, dp(4));
            offBudgetContainer.addView(tv);
            return;
        }

        for (Map.Entry<String, Double> entry : totals.entrySet()) {

            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(3), 0, dp(3));

            TextView left = new TextView(this);
            left.setLayoutParams(new LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f
            ));
            left.setText(entry.getKey());
            left.setTextSize(14);
            left.setTextColor(0xFF333333);

            TextView right = new TextView(this);
            right.setText(
                    money.format(entry.getValue()) + " " + currencySymbol
            );
            right.setTextSize(14);
            right.setTypeface(Typeface.DEFAULT_BOLD);
            right.setTextColor(0xFF222222);

            row.addView(left);
            row.addView(right);
            offBudgetContainer.addView(row);
        }
    }

    /* ========================== HELPERS ============================= */

    private enum Bucket { FIXED, BASIC, DISCRETIONARY, OFF_BUDGET }

    private Bucket bucketOf(String tagRaw) {
        String t = safe(tagRaw);
        if (t.equalsIgnoreCase(TAG_OFF_BUDGET)) return Bucket.OFF_BUDGET;
        if (t.equalsIgnoreCase(TAG_FIXED)) return Bucket.FIXED;
        if (t.equalsIgnoreCase(TAG_BASIC) || t.equalsIgnoreCase(TAG_NECESSITIES))
            return Bucket.BASIC;
        return Bucket.DISCRETIONARY;
    }

    private Map<String, Map<String, Double>> buildTagMonthTotals(List<Expense> items) {
        Map<String, Map<String, Double>> map = new HashMap<>();

        for (Expense e : items) {
            Date d = parseDateSafe(e.date);
            if (d == null) continue;

            String ym = toYearMonthKey(d);
            String tag =
                    CategoryManager.getTagForCategory(this, safe(e.category));
            Bucket b = bucketOf(tag);

            String key =
                    (b == Bucket.FIXED) ? TAG_FIXED :
                            (b == Bucket.BASIC) ? TAG_BASIC :
                                    (b == Bucket.OFF_BUDGET) ? TAG_OFF_BUDGET :
                                            TAG_DISCRETIONARY;

            Map<String, Double> m =
                    map.computeIfAbsent(key, k -> new HashMap<>());

            m.put(ym, m.getOrDefault(ym, 0.0) + e.amount);
        }
        return map;
    }

    private List<String> collectSortedMonthsExcludingOffBudget(
            Map<String, Map<String, Double>> byTagMonth
    ) {
        List<String> months = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : byTagMonth.entrySet()) {
            if (TAG_OFF_BUDGET.equalsIgnoreCase(entry.getKey())) continue;
            for (String key : entry.getValue().keySet())
                if (!months.contains(key)) months.add(key);
        }
        Collections.sort(months);
        return months;
    }

    private double sumBucketForMonth(
            Map<String, Map<String, Double>> map,
            String ym,
            Bucket bucket
    ) {
        String key =
                (bucket == Bucket.FIXED) ? TAG_FIXED :
                        (bucket == Bucket.BASIC) ? TAG_BASIC :
                                TAG_DISCRETIONARY;

        Map<String, Double> months = map.get(key);
        if (months == null) return 0.0;
        return months.getOrDefault(ym, 0.0);
    }

    private void setSafeLegend(TextView v, String text) {
        if (v != null) v.setText(text);
    }

    private String formatDeltaPct(double cur, double prev) {
        if (prev <= 0.0) return "—";
        double pct = ((cur - prev) / prev) * 100.0;
        return String.format(Locale.ENGLISH, "%+.1f%%", pct);
    }

    private Date parseDateSafe(String raw) {
        if (raw == null) return null;
        for (String p : DATE_PATTERNS) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                return f.parse(raw);
            } catch (ParseException ignored) {}
        }
        return null;
    }

    private String toYearMonthKey(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return String.format(
                Locale.ENGLISH,
                "%04d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1
        );
    }

    private String safe(String s) {
        return (s == null) ? "" : s.trim();
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }
}
