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

import android.graphics.Color;
import android.view.Gravity;

public class MonthWiseActivity extends AppCompatActivity {

    private LinearLayout monthContainer;

    private final SimpleDateFormat headerOut = new SimpleDateFormat("MMMM - yyyy", Locale.ENGLISH);
    private final SimpleDateFormat dayOut    = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
    private final SimpleDateFormat rawKeyFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

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

        // Load + apply global display range
        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        List<Expense> filtered = DateRangeCutoff.filterByMonths(this, all);

        // Group actual data by month key
        Map<Long, List<Expense>> byMonth = new LinkedHashMap<>();
        for (Expense e : filtered) {
            Date d = parseDate(e.date);
            if (d == null) continue;
            long key = monthKey(d);
            byMonth.computeIfAbsent(key, k -> new ArrayList<>()).add(e);
        }

        // Stable order inside month
        for (List<Expense> items : byMonth.values()) {
            Collections.sort(items, Comparator.comparingInt(exp -> exp.id));
        }

        // Build 12-month window: current month + previous 11, newest first
        List<Long> window = buildLastNMonthsKeys(12, true);

        monthContainer.removeAllViews();

        int accentText = ContextCompat.getColor(this, R.color.colorAccent2);
        int headerBg   = 0xFFBFCBD3; // medium gray

        for (Long key : window) {
            List<Expense> monthItems = byMonth.get(key);
            Date monthDate = new Date(key);
            String monthLabel = headerOut.format(monthDate);

            double monthTotal = 0.0;
            if (monthItems != null) {
                for (Expense e : monthItems) monthTotal += e.amount;
            }

            // Build per-day rows if present
            Map<String, Double> dayTotalsByRaw = new LinkedHashMap<>();
            Map<String, String> displayLabelForRaw = new LinkedHashMap<>();
            Set<String> uniqueRawKeys = new LinkedHashSet<>();

            if (monthItems != null) {
                for (Expense e : monthItems) {
                    Date d = parseDate(e.date);
                    if (d == null) continue;
                    String rawKey = rawKeyFmt.format(d);
                    String display = dayOut.format(d);

                    uniqueRawKeys.add(rawKey);
                    displayLabelForRaw.put(rawKey, display);

                    double prev = dayTotalsByRaw.containsKey(rawKey) ? dayTotalsByRaw.get(rawKey) : 0.0;
                    dayTotalsByRaw.put(rawKey, prev + e.amount);
                }
            }

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

            // Collapsible section
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sectionLp.setMargins(dp(12), 0, dp(12), dp(8));
            section.setLayoutParams(sectionLp);
            section.setBackgroundColor(0xFFFFFFFF);
            section.setElevation(1f);

            if (rawKeys.isEmpty()) {
                TextView none = new TextView(this);
                none.setPadding(dp(12), dp(10), dp(12), dp(12));
                none.setText("No entries");
                none.setTextSize(14);
                none.setTextColor(0xFF666666);
                section.addView(none);
            } else {
                for (int i = 0; i < rawKeys.size(); i++) {
                    String rawKey = rawKeys.get(i);
                    String dayLabel = displayLabelForRaw.get(rawKey);
                    double dayTotal = dayTotalsByRaw.get(rawKey) != null ? dayTotalsByRaw.get(rawKey) : 0.0;

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
            }

            // 32dp medium-gray header with total at right (ORIGINAL)
            LinearLayout headerRow = new LinearLayout(this);
            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(32));
            headerLp.setMargins(dp(12), dp(8), dp(12), dp(4));
            headerRow.setLayoutParams(headerLp);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setBackgroundColor(headerBg);
            headerRow.setPadding(dp(12), dp(4), dp(12), dp(4));
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

    // Build N months ending at current month (includeCurrent = true) or ending at previous (false).
    // Returns newest-first.
    private List<Long> buildLastNMonthsKeys(int n, boolean includeCurrent) {
        List<Long> keys = new ArrayList<>(n);
        Calendar c = Calendar.getInstance(Locale.ENGLISH);
        // Snap to first of month, midnight
        c.set(Calendar.DAY_OF_MONTH, 1);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (!includeCurrent) {
            c.add(Calendar.MONTH, -1); // start from previous month
        }

        for (int i = 0; i < n; i++) {
            keys.add(c.getTimeInMillis());
            c.add(Calendar.MONTH, -1);
        }

        // ensure newest first (already in newest-first order as we walked backwards from "now")
        return keys;
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
