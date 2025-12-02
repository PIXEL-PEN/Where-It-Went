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
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class DayDetailActivity extends AppCompatActivity {

    private LinearLayout expensesContainer;

    private final SimpleDateFormat incoming = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private final SimpleDateFormat displayOut = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

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
        setContentView(R.layout.activity_day_detail);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        TextView banner = findViewById(R.id.daydetail_banner);
        expensesContainer = findViewById(R.id.daydetail_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        // ----------------------------------------------------
        // FIXED: load proper symbol (not old currency_code)
        // ----------------------------------------------------
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String symbol = prefs.getString("currency_symbol", "$");

        String selectedDateRaw = getIntent().getStringExtra("selected_date");
        if (selectedDateRaw == null) return;

        banner.setText(formatDisplay(selectedDateRaw));

        Date target = parseWith(incoming, selectedDateRaw);
        if (target == null) return;

        Calendar tgt = asYMD(target);

        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        List<Expense> expenses = new ArrayList<>();
        for (Expense e : all) {
            Date d = parseAny(e.date);
            if (d == null) {
                if (selectedDateRaw.equals(e.date)) expenses.add(e);
                continue;
            }
            Calendar cal = asYMD(d);
            if (sameYMD(tgt, cal)) expenses.add(e);
        }

        expensesContainer.removeAllViews();

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
            display.setSpan(new RelativeSizeSpan(0.85f),
                    start,
                    formatted.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            textAmount.setText(display);

            row.setOnClickListener(v -> {
// Get tag for category
                String tag = CategoryManager.getTagForCategory(DayDetailActivity.this, e.category);
                String catLine;

                if (tag != null && !tag.trim().isEmpty()) {
                    catLine = "Category: " + e.category + " (" + tag + ")";
                } else {
                    catLine = "Category: " + e.category;
                }

                String details =
                        catLine + "\n"
                                + "Date: " + safeDisplay(e.date) + "\n"
                                + "Item: " + e.description + "\n"
                                + "Amount: " + String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);

                AlertDialog dialog = new AlertDialog.Builder(DayDetailActivity.this)
                        .setTitle("Expense Details")
                        .setMessage(details)
                        .setNegativeButton("CLOSE", (d, w) -> d.dismiss())
                        .setNeutralButton("DELETE", (d, w) -> {
                            ExpenseDatabase.getDatabase(DayDetailActivity.this)
                                    .expenseDao()
                                    .delete(e);
                            refreshAfterEdit();
                        })
                        .setPositiveButton("EDIT", (d, w) -> {
                            AddExpenseDialog dlg = AddExpenseDialog.newInstance(e.id);
                            dlg.show(getSupportFragmentManager(), "EDIT_EXPENSE");
                        })
                        .create();

                dialog.show();
            });

            expensesContainer.addView(row);

            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(0xFF888888);
            expensesContainer.addView(divider);

            total += e.amount;
        }

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
        int s2 = totalFormatted.length() - symbol.length();
        totalDisplay.setSpan(new RelativeSizeSpan(0.85f),
                s2,
                totalFormatted.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        amountTv.setText(totalDisplay);
        amountTv.setTextSize(18);
        amountTv.setTypeface(Typeface.DEFAULT_BOLD);
        amountTv.setTextColor(0xFFB71C1C);

        totalRow.addView(label);
        totalRow.addView(amountTv);
        expensesContainer.addView(totalRow);
    }

    public void refreshAfterEdit() {
        recreate();
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    private Date parseWith(SimpleDateFormat fmt, String raw) {
        try {
            fmt.setLenient(false);
            return fmt.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private Date parseAny(String raw) {
        if (raw == null) return null;
        for (String p : parsePatterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                return f.parse(raw);
            } catch (Exception ignore) {}
        }
        return null;
    }

    private Calendar asYMD(Date d) {
        Calendar c = Calendar.getInstance(Locale.ENGLISH);
        c.setTime(d);
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        return c;
    }

    private boolean sameYMD(Calendar a, Calendar b) {
        return a.get(Calendar.YEAR) == b.get(Calendar.YEAR)
                && a.get(Calendar.MONTH) == b.get(Calendar.MONTH)
                && a.get(Calendar.DAY_OF_MONTH) == b.get(Calendar.DAY_OF_MONTH);
    }

    private String formatDisplay(String incomingYMD) {
        try {
            Date d = incoming.parse(incomingYMD);
            return displayOut.format(d);
        } catch (Exception e) {
            return incomingYMD;
        }
    }

    private String safeDisplay(String raw) {
        Date d = parseAny(raw);
        if (d != null) return displayOut.format(d);
        return raw;
    }
}
