package com.example.expensetracker;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DayDetailActivity extends AppCompatActivity {

    private LinearLayout expensesContainer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        expensesContainer = findViewById(R.id.daydetail_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        String selectedDate = getIntent().getStringExtra("selected_date");
        if (selectedDate == null) return;

        List<Expense> expenses = ExpenseDatabase
                .getDatabase(this)
                .expenseDao()
                .getByDate(selectedDate);

        expensesContainer.removeAllViews();

        // ✅ Date banner at top
        TextView banner = new TextView(this);
        banner.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(29)
        ));
        banner.setBackgroundColor(0xFFE1C699);
        banner.setText(formatFullDate(selectedDate));
        banner.setTextSize(16);
        banner.setTypeface(Typeface.DEFAULT_BOLD);
        banner.setTextColor(0xFF000000);
        banner.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
        banner.setPadding(dp(16), 0, 0, 0);
        expensesContainer.addView(banner);

        double total = 0.0;

        for (Expense e : expenses) {
            View row = inflater.inflate(R.layout.item_expense_date_row, expensesContainer, false);

            TextView textDescription = row.findViewById(R.id.text_description);
            TextView textCategory   = row.findViewById(R.id.text_category);
            TextView textAmount     = row.findViewById(R.id.text_amount);

            textDescription.setText(e.description);
            textCategory.setText(e.category);

            String formatted = String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);
            SpannableString display = new SpannableString(formatted);
            int start = formatted.length() - symbol.length();
            display.setSpan(new RelativeSizeSpan(0.85f), start, formatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            textAmount.setText(display);

            // Click → edit/delete dialog
            row.setOnClickListener(v -> {
                String details = "Category: " + e.category + "\n"
                        + "Date: " + formatFullDate(e.date) + "\n"
                        + "Item: " + e.description + "\n"
                        + "Amount: " + String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);

                AlertDialog dialog = new AlertDialog.Builder(DayDetailActivity.this)
                        .setTitle("Expense Details")
                        .setMessage(details)
                        .setNegativeButton("CLOSE", (d, which) -> d.dismiss())
                        .setNeutralButton("DELETE", (d, which) -> {
                            ExpenseDatabase.getDatabase(DayDetailActivity.this)
                                    .expenseDao()
                                    .delete(e);
                            recreate();
                        })
                        .setPositiveButton("EDIT", (d, which) -> {
                            Intent intent = new Intent(DayDetailActivity.this, AddExpenseActivity.class);
                            intent.putExtra("expense_id", e.id);
                            startActivity(intent);
                        })
                        .create();

                dialog.show();
            });

            expensesContainer.addView(row);

            // Divider
            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(0xFF888888);
            expensesContainer.addView(divider);

            total += e.amount;
        }

        // TOTAL row
        LinearLayout totalRow = new LinearLayout(this);
        totalRow.setOrientation(LinearLayout.HORIZONTAL);
        totalRow.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView label = new TextView(this);
        label.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        label.setText("TOTAL");
        label.setTextSize(18);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(0xFFB71C1C);

        TextView amountTv = new TextView(this);
        amountTv.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        DecimalFormat df = new DecimalFormat("#,##0.00");
        String totalFormatted = df.format(total) + " " + symbol;

        SpannableString totalDisplay = new SpannableString(totalFormatted);
        int start = totalFormatted.length() - symbol.length();
        totalDisplay.setSpan(
                new RelativeSizeSpan(0.85f),
                start,
                totalFormatted.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        );
        amountTv.setText(totalDisplay);
        amountTv.setTextSize(18);
        amountTv.setTypeface(Typeface.DEFAULT_BOLD);
        amountTv.setTextColor(0xFFB71C1C);

        totalRow.addView(label);
        totalRow.addView(amountTv);
        expensesContainer.addView(totalRow);
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    // ✅ Format YYYY-MM-DD → "25 Sep. 2025"
    private String formatFullDate(String raw) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date d = in.parse(raw);
            SimpleDateFormat out = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
            return out.format(d);
        } catch (Exception e) {
            return raw;
        }
    }
}
