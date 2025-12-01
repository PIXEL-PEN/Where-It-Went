package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DistributionActivity extends AppCompatActivity {

    private static final String TAG_FIXED          = "Fixed";
    private static final String TAG_BASIC          = "Necessity";
    private static final String TAG_NECESSITIES    = "Necessities";
    private static final String TAG_DISCRETIONARY  = "Discretionary";
    private static final String TAG_OTHER          = "Other";
    private static final String TAG_OFF_BUDGET     = "Off-Budget";

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
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(this, MainScreen.class));
            finish();
        });

        SimplePieView pie = findViewById(R.id.pie);

        TextView fixedTotal = findViewById(R.id.legend_fixed_total);
        TextView fixedDelta = findViewById(R.id.legend_fixed_delta);
        TextView basicTotal = findViewById(R.id.legend_basic_total);
        TextView basicDelta = findViewById(R.id.legend_basic_delta);
        TextView discTotal  = findViewById(R.id.legend_disc_total);
        TextView discDelta  = findViewById(R.id.legend_disc_delta);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String currencySymbol = prefs.getString("currency_symbol", "$");
        DecimalFormat money = new DecimalFormat("#,##0.00");

        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        Map<String, Map<String, Double>> byTagMonth = buildTagMonthTotals(all);

        List<String> months = collectSortedMonthsExcludingOffBudget(byTagMonth);
        if (months.isEmpty()) {
            pie.setValues(0f, 0f, 0f);
            fixedTotal.setText("0 " + currencySymbol);
            basicTotal.setText("0 " + currencySymbol);
            discTotal.setText("0 " + currencySymbol);
            fixedDelta.setText("—");
            basicDelta.setText("—");
            discDelta.setText("—");
            return;
        }

        String latestMonth = months.get(months.size() - 1);
        String prevMonth = (months.size() >= 2) ? months.get(months.size() - 2) : null;

        double fixedLatest = sumBucketForMonth(byTagMonth, latestMonth, Bucket.FIXED);
        double basicLatest = sumBucketForMonth(byTagMonth, latestMonth, Bucket.BASIC);
        double discLatest  = sumBucketForMonth(byTagMonth, latestMonth, Bucket.DISCRETIONARY);

        pie.setValues((float) fixedLatest, (float) discLatest, (float) basicLatest);

        fixedTotal.setText(money.format(fixedLatest) + " " + currencySymbol);
        basicTotal.setText(money.format(basicLatest) + " " + currencySymbol);
        discTotal.setText(money.format(discLatest) + " " + currencySymbol);

        if (prevMonth == null) {
            fixedDelta.setText("—");
            basicDelta.setText("—");
            discDelta.setText("—");
        } else {
            double fixedPrev = sumBucketForMonth(byTagMonth, prevMonth, Bucket.FIXED);
            double basicPrev = sumBucketForMonth(byTagMonth, prevMonth, Bucket.BASIC);
            double discPrev  = sumBucketForMonth(byTagMonth, prevMonth, Bucket.DISCRETIONARY);

            fixedDelta.setText(formatDeltaPct(fixedLatest, fixedPrev));
            basicDelta.setText(formatDeltaPct(basicLatest, basicPrev));
            discDelta.setText(formatDeltaPct(discLatest, discPrev));
        }

        // ---------------------------------------------------------
        //  OFF-BUDGET SECTION
        // ---------------------------------------------------------

        LinearLayout offBudgetContainer = findViewById(R.id.off_budget_container);
        offBudgetContainer.removeAllViews();

        // Collect Off-Budget totals
        Map<String, Double> map = new LinkedHashMap<>();
        for (Expense e : all) {
            if (e.category == null) continue;

            String tag = CategoryManager.getTagForCategory(this, e.category);
            if (!CategoryManager.TAG_OFF.equalsIgnoreCase(tag)) continue;

            String key = (e.description == null || e.description.trim().isEmpty())
                    ? "(item)"
                    : e.description.trim();

            double amt = e.amount;
            map.put(key, map.getOrDefault(key, 0.0) + amt);
        }

        if (map.isEmpty()) {
            TextView tv = new TextView(this);
            tv.setText("No Off-Budget items");
            tv.setTextSize(15);
            tv.setTextColor(0xFF555555);
            tv.setPadding(0, dp(6), 0, dp(4));
            offBudgetContainer.addView(tv);
        } else {

            // Divider line
            View line = new View(this);
            line.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(1)
            ));
            line.setBackgroundColor(0xFFCCCCCC);
            offBudgetContainer.addView(line);

            // Header
            TextView header = new TextView(this);
            header.setText("Off-Budget");
            header.setTextSize(16);
            header.setTypeface(Typeface.DEFAULT_BOLD);
            header.setTextColor(0xFF222222);
            header.setPadding(0, dp(2), 0, dp(4));
            offBudgetContainer.addView(header);

            // Rows
            for (Map.Entry<String, Double> entry : map.entrySet()) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                row.setPadding(0, dp(3), 0, dp(3));

                TextView left = new TextView(this);
                left.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                left.setText(entry.getKey());
                left.setTextSize(14);
                left.setTextColor(0xFF333333);

                TextView right = new TextView(this);
                right.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT));
                right.setText(money.format(entry.getValue()) + " " + currencySymbol);
                right.setTextSize(14);
                right.setTypeface(Typeface.DEFAULT_BOLD);
                right.setTextColor(0xFF222222);

                row.addView(left);
                row.addView(right);
                offBudgetContainer.addView(row);
            }
        }
    }

    private enum Bucket { FIXED, BASIC, DISCRETIONARY }

    private Bucket bucketOf(String tagRaw) {
        String t = safe(tagRaw);
        if (t.equalsIgnoreCase(TAG_FIXED)) return Bucket.FIXED;
        if (t.equalsIgnoreCase(TAG_BASIC) || t.equalsIgnoreCase(TAG_NECESSITIES)) return Bucket.BASIC;
        return Bucket.DISCRETIONARY;
    }

    private Map<String, Map<String, Double>> buildTagMonthTotals(List<Expense> items) {
        Map<String, Map<String, Double>> map = new HashMap<>();
        for (Expense e : items) {
            Date d = parseDateSafe(e.date);
            if (d == null) continue;

            String ym = toYearMonthKey(d);
            String tag = CategoryManager.getTagForCategory(this, safe(e.category));
            if (CategoryManager.TAG_OFF.equalsIgnoreCase(tag)) continue;

            String normalizedTag = tag.equalsIgnoreCase(TAG_BASIC) ? TAG_BASIC :
                    tag.equalsIgnoreCase(TAG_FIXED) ? TAG_FIXED :
                            TAG_DISCRETIONARY;

            Map<String, Double> monthTotals = map.computeIfAbsent(normalizedTag,
                    k -> new HashMap<>());

            monthTotals.put(ym, monthTotals.getOrDefault(ym, 0.0) + e.amount);
        }
        return map;
    }

    private List<String> collectSortedMonthsExcludingOffBudget(Map<String, Map<String, Double>> byTagMonth) {
        List<String> months = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> e : byTagMonth.entrySet()) {
            for (String ym : e.getValue().keySet()) {
                if (!months.contains(ym)) months.add(ym);
            }
        }
        Collections.sort(months);
        return months;
    }

    private double sumBucketForMonth(Map<String, Map<String, Double>> byTagMonth, String ym, Bucket bucket) {
        String key = (bucket == Bucket.FIXED) ? TAG_FIXED :
                (bucket == Bucket.BASIC) ? TAG_BASIC : TAG_DISCRETIONARY;

        Map<String, Double> months = byTagMonth.get(key);
        if (months == null) return 0.0;
        Double v = months.get(ym);
        return v == null ? 0.0 : v;
    }

    private String formatDeltaPct(double cur, double prev) {
        if (prev <= 0) return "—";
        double pct = ((cur - prev) / prev) * 100;
        return String.format(Locale.ENGLISH, "%+.1f%%", pct);
    }

    private Date parseDateSafe(String raw) {
        if (raw == null) return null;
        for (String p : DATE_PATTERNS) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                return f.parse(raw);
            } catch (Exception ignore) {}
        }
        return null;
    }

    private String toYearMonthKey(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        return String.format(Locale.ENGLISH, "%04d-%02d",
                c.get(Calendar.YEAR),
                c.get(Calendar.MONTH) + 1);
    }

    private String safe(String s) { return (s == null) ? "" : s.trim(); }

    private int dp(int v) {
        return Math.round(getResources().getDisplayMetrics().density * v);
    }
}
