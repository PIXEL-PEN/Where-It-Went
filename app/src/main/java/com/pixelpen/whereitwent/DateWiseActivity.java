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
import androidx.core.content.ContextCompat;

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

        int accentColor = ContextCompat.getColor(this, R.color.colorAccent2);

        for (String rawDate : dates) {
            List<Expense> items = grouped.get(rawDate);

            double total = 0.0;
            for (Expense e : items) total += e.amount;

            String headerLabel = formatFullDate(rawDate);

            // Section content container for this day (collapsible)
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sectionLp.setMargins(dp(12), 0, dp(12), dp(8));
            section.setLayoutParams(sectionLp);
            section.setBackgroundColor(0xFFFFFFFF);
            section.setElevation(1f);

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
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                    lp.setMargins(dp(12), 0, dp(12), 0);
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(0x1A000000);
                    section.addView(divider);
                }
            }

            // Per-day TOTAL row (inside section)
            LinearLayout totalRow = new LinearLayout(this);
            totalRow.setOrientation(LinearLayout.HORIZONTAL);
            totalRow.setPadding(dp(12), dp(10), dp(12), dp(12));

            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            label.setText("TOTAL");
            label.setTextSize(15);
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
            amountTv.setTextSize(15);
            amountTv.setTypeface(Typeface.DEFAULT_BOLD);
            amountTv.setTextColor(0xFFB71C1C);

            totalRow.addView(label);
            totalRow.addView(amountTv);
            section.addView(totalRow);

            // Header row (compact, styled like your View buttons)
            LinearLayout headerRow = new LinearLayout(this);
            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
            headerLp.setMargins(dp(12), dp(8), dp(12), dp(4));
            headerRow.setLayoutParams(headerLp);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setBackgroundColor(0xFFE0E0E0);
            headerRow.setPadding(dp(14), dp(6), dp(14), dp(6));
            headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);

            // Left: date (bold 16sp) + count (normal 14sp)
            TextView leftLabel = new TextView(this);
            leftLabel.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            leftLabel.setTextColor(accentColor);

// Set base size to 16sp so date appears 16
            leftLabel.setTextSize(16);
            leftLabel.setTypeface(Typeface.DEFAULT);   // keep normal so spans work

            String datePart = headerLabel;
            String countPart = " (" + items.size() + ")";
            SpannableString labelSpan = new SpannableString(datePart + countPart);

// Bold only the date
            labelSpan.setSpan(
                    new android.text.style.StyleSpan(Typeface.BOLD),
                    0,
                    datePart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

// Shrink only the count to ~14sp
// 14/16 ≈ 0.875
            labelSpan.setSpan(
                    new RelativeSizeSpan(0.875f),
                    datePart.length(),
                    datePart.length() + countPart.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );

            leftLabel.setText(labelSpan);


            // Right: total (bold, 1sp smaller)
            TextView rightTotal = new TextView(this);
            rightTotal.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            rightTotal.setText(money.format(total) + " " + symbol);
            rightTotal.setTextSize(14);
            rightTotal.setTypeface(Typeface.DEFAULT_BOLD);
            rightTotal.setTextColor(accentColor);

            headerRow.addView(leftLabel);
            headerRow.addView(rightTotal);

            // Default: all collapsed
            section.setVisibility(View.GONE);

            headerRow.setOnClickListener(v -> {
                section.setVisibility(section.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });

            expensesContainer.addView(headerRow);
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
