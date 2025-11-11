package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MonthWiseActivity extends AppCompatActivity {

    private LinearLayout monthContainer;

    // Header and day display formats
    private final SimpleDateFormat headerOut = new SimpleDateFormat("MMMM - yyyy", Locale.ENGLISH); // e.g., "November - 2025"
    private final SimpleDateFormat dayOut    = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH); // e.g., "12 Nov. 2025"
    private final SimpleDateFormat rawKeyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);   // DayDetail expects this

    // Parsers for app's mixed input formats
    private final String[] parsePatterns = new String[] {
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_month_wise);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        monthContainer = findViewById(R.id.monthwise_container);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        LayoutInflater inflater = LayoutInflater.from(this);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);
        DecimalFormat money = new DecimalFormat("#,##0.00");

        // Load then apply global Date Range (display only)
        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        List<Expense> filtered = DateRangeCutoff.filterByMonths(this, all);

        // Group by month (key = millis at first day of month)
        Map<Long, List<Expense>> byMonth = new LinkedHashMap<>();
        for (Expense e : filtered) {
            Date d = parseDate(e.date);
            if (d == null) continue;
            long key = monthKey(d);
            byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        // Stable item order inside month (oldest → newest by id)
        for (List<Expense> items : byMonth.values()) {
            Collections.sort(items, Comparator.comparingInt(exp -> exp.id));
        }

        // Newest month first
        List<Long> keys = new ArrayList<>(byMonth.keySet());
        Collections.sort(keys, (k1, k2) -> Long.compare(k2, k1));

        monthContainer.removeAllViews();

        int accentText = ContextCompat.getColor(this, R.color.colorAccent2);
        int headerBg   = 0xFFC9D6DF; // darker header background per your request

        for (Long key : keys) {
            List<Expense> monthItems = byMonth.get(key);
            if (monthItems == null || monthItems.isEmpty()) continue;

            Date monthDate = new Date(key);
            String monthLabel = headerOut.format(monthDate);

            double monthTotal = 0.0;
            for (Expense e : monthItems) monthTotal += e.amount;

            // ---- Build unique day list with totals and raw keys ----
            // Use LinkedHash* to preserve insertion order before sorting explicitly.
            Map<String, Double> dayTotalsByRaw = new LinkedHashMap<>(); // rawKey "yyyy-MM-dd" -> total
            Map<String, String> displayLabelForRaw = new LinkedHashMap<>(); // rawKey -> "dd MMM. yyyy"
            Set<String> uniqueRawKeys = new LinkedHashSet<>();

            for (Expense e : monthItems) {
                Date d = parseDate(e.date);
                if (d == null) continue;
                String rawKey = rawKeyFmt.format(d); // EXACT key DayDetail expects
                String display = dayOut.format(d);

                uniqueRawKeys.add(rawKey);
                displayLabelForRaw.put(rawKey, display);

                double prev = dayTotalsByRaw.containsKey(rawKey) ? dayTotalsByRaw.get(rawKey) : 0.0;
                dayTotalsByRaw.put(rawKey, prev + e.amount);
            }

            // Sort days newest → oldest by parsing rawKey
            List<String> rawKeys = new ArrayList<>(uniqueRawKeys);
            Collections.sort(rawKeys, (a, b) -> {
                try {
                    Date da = rawKeyFmt.parse(a);
                    Date db = rawKeyFmt.parse(b);
                    if (da == null || db == null) return b.compareTo(a);
                    return db.compareTo(da);
                } catch (Exception ex) {
                    return b.compareTo(a);
                }
            });

            // ---- Collapsible section with day rows ----
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sectionLp.setMargins(dp(12), 0, dp(12), dp(8));
            section.setLayoutParams(sectionLp);
            section.setBackgroundColor(0xFFFFFFFF);
            section.setElevation(1f);

            for (int i = 0; i < rawKeys.size(); i++) {
                String rawKey = rawKeys.get(i);                 // yyyy-MM-dd
                String dayLabel = displayLabelForRaw.get(rawKey); // dd MMM. yyyy
                double dayTotal = dayTotalsByRaw.get(rawKey) != null ? dayTotalsByRaw.get(rawKey) : 0.0;

                // Row: day label left, amount right
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(dp(12), dp(12), dp(12), dp(12));
                row.setClickable(true);

                TextView tvLeft = new TextView(this);
                tvLeft.setLayoutParams(new LinearLayout.LayoutParams(
                        0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
                tvLeft.setText(dayLabel);
                tvLeft.setTextSize(15);
                tvLeft.setTypeface(Typeface.DEFAULT_BOLD);
                tvLeft.setTextColor(0xFF222222);

                TextView tvRight = new TextView(this);
                tvRight.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
                String amt = money.format(dayTotal) + " " + symbol;
                SpannableString amtSpan = new SpannableString(amt);
                int s = amt.length() - symbol.length();
                amtSpan.setSpan(new RelativeSizeSpan(0.90f), s, amt.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tvRight.setText(amtSpan);
                tvRight.setTextSize(15);
                tvRight.setTypeface(Typeface.DEFAULT_BOLD);
                tvRight.setTextColor(0xFF222222);

                row.addView(tvLeft);
                row.addView(tvRight);

                // Click: launch DayDetailActivity with EXACT raw key "yyyy-MM-dd"
                row.setOnClickListener(v -> {
                    Intent intent = new Intent(MonthWiseActivity.this, DayDetailActivity.class);
                    intent.putExtra("selected_date", rawKey);
                    startActivity(intent);
                });

                section.addView(row);

                if (i < rawKeys.size() - 1) {
                    View divider = new View(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                    lp.setMargins(dp(12), 0, dp(12), 0);
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(0x1A000000);
                    section.addView(divider);
                }
            }

            // ---- Month header (bold month left, month total right, darker bg) ----
            LinearLayout headerRow = new LinearLayout(this);
            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
            headerLp.setMargins(dp(12), dp(8), dp(12), dp(4));
            headerRow.setLayoutParams(headerLp);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setBackgroundColor(headerBg);
            headerRow.setPadding(dp(12), dp(6), dp(12), dp(6));
            headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            headerRow.setClickable(true);

            TextView leftMonth = new TextView(this);
            leftMonth.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            leftMonth.setText(monthLabel);
            leftMonth.setTextSize(16);
            leftMonth.setTypeface(Typeface.DEFAULT_BOLD);
            leftMonth.setTextColor(accentText);

            TextView rightTotal = new TextView(this);
            rightTotal.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            rightTotal.setText(money.format(monthTotal) + " " + symbol);
            rightTotal.setTextSize(14);
            rightTotal.setTypeface(Typeface.DEFAULT_BOLD);
            rightTotal.setTextColor(accentText);

            headerRow.addView(leftMonth);
            headerRow.addView(rightTotal);

            // Default collapsed
            section.setVisibility(View.GONE);
            headerRow.setOnClickListener(v -> {
                section.setVisibility(section.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });

            monthContainer.addView(headerRow);
            monthContainer.addView(section);
        }
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    private Date parseDate(String raw) {
        for (String p : parsePatterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(p, Locale.ENGLISH);
                in.setLenient(false);
                return in.parse(raw);
            } catch (Exception ignore) {}
        }
        return null;
    }

    private long monthKey(Date d) {
        Calendar c = Calendar.getInstance(Locale.ENGLISH);
        c.setTime(d);
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c.getTimeInMillis();
    }
}
