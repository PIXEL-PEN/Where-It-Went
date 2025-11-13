package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DistributionActivity extends AppCompatActivity {

    // Buckets we visualize
    private static final String TAG_FIXED          = "Fixed";
    private static final String TAG_BASIC          = "Necessity";
    private static final String TAG_NECESSITIES    = "Necessities";   // legacy alias you used earlier
    private static final String TAG_DISCRETIONARY  = "Discretionary";
    private static final String TAG_OTHER          = "Other";          // safety catch-all
    private static final String TAG_OFF_BUDGET     = "Off-Budget";     // excluded from chart

    private static final String[] DATE_PATTERNS = new String[] {
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

        ImageButton back = findViewById(R.id.btn_back);
        if (back != null) back.setOnClickListener(v -> {
            startActivity(new Intent(this, MainActivity.class));
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
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);
        DecimalFormat money = new DecimalFormat("#,##0.##");

        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        // Build: tag -> (yyyy-MM -> total)
        Map<String, Map<String, Double>> byTagMonth = buildTagMonthTotals(all);

        // Collect all months that have any non–Off-Budget value (so empty project-only months don’t show)
        List<String> months = collectSortedMonthsExcludingOffBudget(byTagMonth);
        if (months.isEmpty()) {
            pie.setValues(0f, 0f, 0f);
            if (fixedTotal != null) fixedTotal.setText("0 " + symbol);
            if (basicTotal != null) basicTotal.setText("0 " + symbol);
            if (discTotal  != null) discTotal.setText("0 " + symbol);
            if (fixedDelta != null) fixedDelta.setText("—");
            if (basicDelta != null) basicDelta.setText("—");
            if (discDelta  != null) discDelta.setText("—");
            return;
        }

        String latestMonth = months.get(months.size() - 1);
        String prevMonth   = (months.size() >= 2) ? months.get(months.size() - 2) : null;

        double fixedLatest = sumBucketForMonth(byTagMonth, latestMonth, Bucket.FIXED);
        double basicLatest = sumBucketForMonth(byTagMonth, latestMonth, Bucket.BASIC);
        double discLatest  = sumBucketForMonth(byTagMonth, latestMonth, Bucket.DISCRETIONARY);

        // Push to pie (Off-Budget is excluded by construction)
        // If SimplePieView draws slices in order: [Fixed, Discretionary, Necessity]
        pie.setValues((float) fixedLatest, (float) discLatest, (float) basicLatest);


        if (fixedTotal != null) fixedTotal.setText(money.format(fixedLatest) + " " + symbol);
        if (basicTotal != null) basicTotal.setText(money.format(basicLatest) + " " + symbol);
        if (discTotal  != null) discTotal.setText(money.format(discLatest) + " " + symbol);

        if (prevMonth == null) {
            if (fixedDelta != null) fixedDelta.setText("—");
            if (basicDelta != null) basicDelta.setText("—");
            if (discDelta  != null) discDelta.setText("—");
            return;
        }

        double fixedPrev = sumBucketForMonth(byTagMonth, prevMonth, Bucket.FIXED);
        double basicPrev = sumBucketForMonth(byTagMonth, prevMonth, Bucket.BASIC);
        double discPrev  = sumBucketForMonth(byTagMonth, prevMonth, Bucket.DISCRETIONARY);

        if (fixedDelta != null) fixedDelta.setText(formatDeltaPct(fixedLatest, fixedPrev));
        if (basicDelta != null) basicDelta.setText(formatDeltaPct(basicLatest, basicPrev));
        if (discDelta  != null)  discDelta.setText(formatDeltaPct(discLatest,  discPrev));
    }

    private enum Bucket { FIXED, BASIC, DISCRETIONARY, OFF_BUDGET, IGNORE }

    private Bucket bucketOf(String tagRaw) {
        String t = safe(tagRaw);
        if (t.equalsIgnoreCase(TAG_OFF_BUDGET))    return Bucket.OFF_BUDGET;
        if (t.equalsIgnoreCase(TAG_FIXED))         return Bucket.FIXED;
        if (t.equalsIgnoreCase(TAG_BASIC) || t.equalsIgnoreCase(TAG_NECESSITIES)) return Bucket.BASIC;
        if (t.equalsIgnoreCase(TAG_DISCRETIONARY) || t.equalsIgnoreCase(TAG_OTHER)) return Bucket.DISCRETIONARY;
        // Unknown/custom tags default to Discretionary (historical behavior), but NOT Off-Budget
        return Bucket.DISCRETIONARY;
    }

    private Map<String, Map<String, Double>> buildTagMonthTotals(List<Expense> items) {
        Map<String, Map<String, Double>> map = new HashMap<>();
        for (Expense e : items) {
            Date d = parseDateSafe(e.date);
            if (d == null) continue;

            String ym = toYearMonthKey(d);
            String tag = CategoryManager.getTagForCategory(this, safe(e.category));
            Bucket b = bucketOf(tag);
            if (b == Bucket.OFF_BUDGET) {
                // Store but separate by its explicit tag for completeness; we’ll exclude later.
                Map<String, Double> monthTotals = map.computeIfAbsent(TAG_OFF_BUDGET, k -> new HashMap<>());
                double cur = monthTotals.getOrDefault(ym, 0.0);
                monthTotals.put(ym, cur + e.amount);
                continue;
            }

            String normalizedTag;
            switch (b) {
                case FIXED:         normalizedTag = TAG_FIXED; break;
                case BASIC:         normalizedTag = TAG_BASIC; break;
                case DISCRETIONARY: normalizedTag = TAG_DISCRETIONARY; break;
                default:            normalizedTag = TAG_DISCRETIONARY; break;
            }

            Map<String, Double> monthTotals = map.computeIfAbsent(normalizedTag, k -> new HashMap<>());
            double cur = monthTotals.getOrDefault(ym, 0.0);
            monthTotals.put(ym, cur + e.amount);
        }
        return map;
    }

    private List<String> collectSortedMonthsExcludingOffBudget(Map<String, Map<String, Double>> byTagMonth) {
        List<String> months = new ArrayList<>();
        for (Map.Entry<String, Map<String, Double>> entry : byTagMonth.entrySet()) {
            String tag = entry.getKey();
            if (TAG_OFF_BUDGET.equalsIgnoreCase(tag)) continue; // skip Off-Budget months
            for (String ym : entry.getValue().keySet()) {
                if (!months.contains(ym)) months.add(ym);
            }
        }
        Collections.sort(months, new Comparator<String>() {
            @Override public int compare(String a, String b) { return a.compareTo(b); }
        });
        return months;
    }

    private double sumBucketForMonth(Map<String, Map<String, Double>> byTagMonth, String ym, Bucket bucket) {
        String key;
        switch (bucket) {
            case FIXED:         key = TAG_FIXED; break;
            case BASIC:         key = TAG_BASIC; break;
            case DISCRETIONARY: key = TAG_DISCRETIONARY; break;
            default:            return 0.0;
        }
        Map<String, Double> months = byTagMonth.get(key);
        if (months == null) return 0.0;
        Double v = months.get(ym);
        return v == null ? 0.0 : v;
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
            } catch (ParseException ignore) {}
        }
        return null;
    }

    private String toYearMonthKey(Date date) {
        Calendar c = Calendar.getInstance();
        c.setTime(date);
        int y = c.get(Calendar.YEAR);
        int m = c.get(Calendar.MONTH) + 1;
        return String.format(Locale.ENGLISH, "%04d-%02d", y, m);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
