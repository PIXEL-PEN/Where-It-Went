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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DateWiseActivity extends AppCompatActivity {

    private LinearLayout expensesContainer;
    private final SimpleDateFormat outHeader = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_date_wise);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        expensesContainer = findViewById(R.id.datewise_container);
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

        // Load all, then apply global Date Range (display only)
        List<Expense> allExpenses = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        List<Expense> filtered = DateRangeCutoff.filterByMonths(this, allExpenses);

        // Group by raw date string
        Map<String, List<Expense>> grouped = new LinkedHashMap<>();
        for (Expense e : filtered) {
            grouped.computeIfAbsent(e.date, k -> new ArrayList<>()).add(e);
        }

        // Sort items within each date (oldest → newest by id)
        for (List<Expense> items : grouped.values()) {
            Collections.sort(items, Comparator.comparingInt(exp -> exp.id));
        }

        // Sort date groups by parsed date (newest → oldest)
        List<String> dates = new ArrayList<>(grouped.keySet());
        Collections.sort(dates, (d1, d2) -> {
            Date date1 = parseDate(d1);
            Date date2 = parseDate(d2);
            if (date1 == null || date2 == null) return d1.compareTo(d2);
            return date2.compareTo(date1);
        });

        expensesContainer.removeAllViews();

        String todayLabel = outHeader.format(new Date());

        for (String rawDate : dates) {
            List<Expense> items = grouped.get(rawDate);

            double total = 0.0;
            for (Expense e : items) total += e.amount;

            String headerLabel = formatFullDate(rawDate);
            String headerRight = " (" + items.size() + ")  " + money.format(total) + " " + symbol;

            // Header "card"
            TextView banner = new TextView(this);
            LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(56));
            bannerLp.setMargins(dp(8), dp(8), dp(8), dp(2));
            banner.setLayoutParams(bannerLp);
            banner.setBackgroundColor(0xFFFFFFFF);
            banner.setPadding(dp(16), 0, dp(12), 0);
            banner.setTypeface(Typeface.DEFAULT_BOLD);
            banner.setTextSize(16);
            banner.setTextColor(0xFF000000);
            banner.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            banner.setElevation(2f);
            banner.setText(headerLabel + headerRight);

            // Section content container for this day
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sectionLp.setMargins(dp(8), 0, dp(8), dp(6));
            section.setLayoutParams(sectionLp);
            section.setBackgroundColor(0xFFFFFFFF);
            section.setElevation(2f);

            // Fill rows
            for (int i = 0; i < items.size(); i++) {
                Expense e = items.get(i);
                View row = inflater.inflate(R.layout.item_expense_date_row, section, false);

                TextView textDescription = row.findViewById(R.id.text_description);
                TextView textCategory   = row.findViewById(R.id.text_category);
                TextView textAmount     = row.findViewById(R.id.text_amount);

                if (textDescription == null || textCategory == null || textAmount == null) {
                    continue;
                }

                textDescription.setText(e.description);
                textCategory.setText(e.category);

                String amt = String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);
                SpannableString amtSpan = new SpannableString(amt);
                int start = amt.length() - symbol.length();
                amtSpan.setSpan(new RelativeSizeSpan(0.85f), start, amt.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textAmount.setText(amtSpan);

                row.setOnClickListener(v -> {
                    String details = "Category: " + e.category + "\n"
                            + "Date: " + formatFullDate(e.date) + "\n"
                            + "Item: " + e.description + "\n"
                            + "Amount: " + String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);

                    AlertDialog dialog = new AlertDialog.Builder(DateWiseActivity.this)
                            .setTitle("Expense Details")
                            .setMessage(details)
                            .setNegativeButton("CLOSE", (d, which) -> d.dismiss())
                            .setNeutralButton("DELETE", (d, which) -> {
                                ExpenseDatabase.getDatabase(DateWiseActivity.this)
                                        .expenseDao()
                                        .delete(e);
                                recreate();
                            })
                            .setPositiveButton("EDIT", (d, which) -> {
                                Intent intent = new Intent(DateWiseActivity.this, AddExpenseActivity.class);
                                intent.putExtra("expense_id", e.id);
                                startActivity(intent);
                            })
                            .create();
                    dialog.show();
                });

                section.addView(row);

                if (i < items.size() - 1) {
                    View divider = new View(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, 1);
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(0xFFCCCCCC);
                    section.addView(divider);
                }
            }

            // Per-day TOTAL row (inside the collapsible section)
            LinearLayout totalRow = new LinearLayout(this);
            totalRow.setOrientation(LinearLayout.HORIZONTAL);
            totalRow.setPadding(dp(12), dp(12), dp(12), dp(12));

            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            label.setText("TOTAL");
            label.setTextSize(16);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(0xFFB71C1C);

            TextView amountTv = new TextView(this);
            amountTv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT));

            String totalFormatted = money.format(total) + " " + symbol;
            SpannableString totalDisplay = new SpannableString(totalFormatted);
            int tStart = totalFormatted.length() - symbol.length();
            totalDisplay.setSpan(new RelativeSizeSpan(0.85f), tStart, totalFormatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            amountTv.setText(totalDisplay);
            amountTv.setTextSize(16);
            amountTv.setTypeface(Typeface.DEFAULT_BOLD);
            amountTv.setTextColor(0xFFB71C1C);

            totalRow.addView(label);
            totalRow.addView(amountTv);
            section.addView(totalRow);

            // Default collapsed except "today"
            boolean expand = headerLabel.equals(todayLabel);
            section.setVisibility(expand ? View.VISIBLE : View.GONE);

            // Toggle on header tap
            banner.setOnClickListener(v -> {
                if (section.getVisibility() == View.VISIBLE) {
                    section.setVisibility(View.GONE);
                } else {
                    section.setVisibility(View.VISIBLE);
                }
            });

            expensesContainer.addView(banner);
            expensesContainer.addView(section);
        }
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    private Date parseDate(String raw) {
        String[] patterns = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "dd MMM yyyy",
                "dd MMM. yyyy",
                "d MMM yyyy",
                "d MMM. yyyy",
                "d MMMM yyyy"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(p, Locale.ENGLISH);
                in.setLenient(false);
                return in.parse(raw);
            } catch (Exception ignore) {}
        }
        return null;
    }

    private String formatFullDate(String raw) {
        Date d = parseDate(raw);
        if (d != null) {
            return outHeader.format(d);
        }
        return raw;
    }
}
