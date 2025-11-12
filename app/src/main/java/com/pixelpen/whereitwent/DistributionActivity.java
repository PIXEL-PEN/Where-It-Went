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

        Map<String, Map<String, Double>> byTagMonth = buildTagMonthTotals(all);

        List<String> months = collectSortedMonths(byTagMonth);
        if (months.isEmpty()) {
            pie.setValues(0f, 0f, 0f);
            if (fixedTotal != null) fixedTotal.setText("0 " + symbol);
            if (basicTotal != null) basicTotal.setText("0 " + symbol);
            if (discTotal  != null)  discTotal.setText("0 " + symbol);
            if (fixedDelta != null) fixedDelta.setText("—");
            if (basicDelta != null) basicDelta.setText("—");
            if (discDelta  != null)  discDelta.setText("—");
            return;
        }

        String latestMonth = months.get(months.size() - 1);
        String prevMonth   = (months.size() >= 2) ? months.get(months.size() - 2) : null;

        double fixedLatest = getMonthTagTotal(byTagMonth, "Fixed", latestMonth);
        double basicLatest = getMonthTagTotal(byTagMonth, "Basic", latestMonth)
                + getMonthTagTotal(byTagMonth, "Necessities", latestMonth);
        double discLatest  = getMonthTagTotal(byTagMonth, "Discretionary", latestMonth)
                + getMonthTagTotal(byTagMonth, "Other", latestMonth); // safety for non-mapped customs

        pie.setValues((float) fixedLatest, (float) basicLatest, (float) discLatest);

        if (fixedTotal != null) fixedTotal.setText(money.format(fixedLatest) + " " + symbol);
        if (basicTotal != null) basicTotal.setText(money.format(basicLatest) + " " + symbol);
        if (discTotal  != null) discTotal.setText(money.format(discLatest) + " " + symbol);

        if (prevMonth == null) {
            if (fixedDelta != null) fixedDelta.setText("—");
            if (basicDelta != null) basicDelta.setText("—");
            if (discDelta  != null) discDelta.setText("—");
            return;
        }

        double fixedPrev = getMonthTagTotal(byTagMonth, "Fixed", prevMonth);
        double basicPrev = getMonthTagTotal(byTagMonth, "Basic", prevMonth)
                + getMonthTagTotal(byTagMonth, "Necessities", prevMonth);
        double discPrev  = getMonthTagTotal(byTagMonth, "Discretionary", prevMonth)
                + getMonthTagTotal(byTagMonth, "Other", prevMonth);

        if (fixedDelta != null) fixedDelta.setText(formatDeltaPct(fixedLatest, fixedPrev));
        if (basicDelta != null) basicDelta.setText(formatDeltaPct(basicLatest, basicPrev));
        if (discDelta  != null)  discDelta.setText(formatDeltaPct(discLatest,  discPrev));
    }

    private Map<String, Map<String, Double>> buildTagMonthTotals(List<Expense> items) {
        Map<String, Map<String, Double>> map = new HashMap<>();
        for (Expense e : items) {
            Date d = parseDateSafe(e.date);
            if (d == null) continue;

            String ym = toYearMonthKey(d);
            String tag = CategoryManager.getTagForCategory(this, safe(e.category));

            Map<String, Double> monthTotals = map.computeIfAbsent(tag, k -> new HashMap<>());
            double cur = monthTotals.getOrDefault(ym, 0.0);
            monthTotals.put(ym, cur + e.amount);
        }
        return map;
    }

    private List<String> collectSortedMonths(Map<String, Map<String, Double>> byTagMonth) {
        List<String> months = new ArrayList<>();
        for (Map<String, Double> m : byTagMonth.values()) {
            for (String ym : m.keySet()) {
                if (!months.contains(ym)) months.add(ym);
            }
        }
        Collections.sort(months, new Comparator<String>() {
            @Override public int compare(String a, String b) {
                // format "yyyy-MM" -> lexical compare works, but keep explicit
                return a.compareTo(b);
            }
        });
        return months;
    }

    private double getMonthTagTotal(Map<String, Map<String, Double>> byTagMonth, String tag, String ym) {
        Map<String, Double> months = byTagMonth.get(tag);
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
