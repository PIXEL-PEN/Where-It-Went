package com.pixelpen.whereitwent;

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
import java.util.*;

public class DayDetailActivity extends AppCompatActivity {

    private LinearLayout expensesContainer;

    // Expected format: yyyy-MM-dd
    private final SimpleDateFormat incoming = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
    private final SimpleDateFormat display  = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

    private final String[] parsePatterns = new String[]{
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

        expensesContainer = findViewById(R.id.daydetail_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        String selected = getIntent().getStringExtra("selected_date");
        if (selected == null) return;

        // Set banner
        TextView banner = findViewById(R.id.daydetail_banner);
        banner.setText(formatDisplay(selected));

        Date target = parseWith(incoming, selected);
        if (target == null) return;

        Calendar tgt = asYMD(target);

        // Load ALL expenses
        ExpenseDao dao = ExpenseDatabase.getDatabase(this).expenseDao();
        List<Expense> all = dao.getAll();
        List<Expense> today = new ArrayList<>();

        for (Expense e : all) {
            Date d = parseAny(e.date);
            if (d == null) {
                if (selected.equals(e.date)) today.add(e);
                continue;
            }
            Calendar cal = asYMD(d);
            if (sameYMD(tgt, cal))
                today.add(e);
        }

        expensesContainer.removeAllViews();

        double total = 0.0;

        for (Expense e : today) {

            View row = inflater.inflate(R.layout.item_expense_date_row, expensesContainer, false);

            TextView dsc = row.findViewById(R.id.text_description);
            TextView cat = row.findViewById(R.id.text_category);
            TextView amt = row.findViewById(R.id.text_amount);

            dsc.setText(e.description);
            cat.setText(e.category);

            String formatted = String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);
            SpannableString dsp = new SpannableString(formatted);
            dsp.setSpan(new RelativeSizeSpan(0.85f),
                    formatted.length() - symbol.length(),
                    formatted.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            amt.setText(dsp);

            row.setOnClickListener(v -> openDetailDialog(e, selected));

            expensesContainer.addView(row);

            // Divider
            View div = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, 1);
            div.setLayoutParams(lp);
            div.setBackgroundColor(0xFF888888);
            expensesContainer.addView(div);

            total += e.amount;
        }

        addTotalRow(total, symbol);
    }

    // ------------------------------------------------------------
    // Expense Details Dialog
    // ------------------------------------------------------------
    private void openDetailDialog(Expense e, String selectedIso) {

        String details = "Category: " + e.category + "\n"
                + "Date: " + safeDisplay(e.date) + "\n"
                + "Item: " + e.description + "\n"
                + "Amount: " + e.amount;

        new AlertDialog.Builder(this)
                .setTitle("Expense Details")
                .setMessage(details)
                .setNegativeButton("CLOSE", null)
                .setNeutralButton("DELETE", (d, w) -> {

                    ExpenseDao dao = ExpenseDatabase.getDatabase(this).expenseDao();
                    dao.delete(e);

                    String ym = selectedIso.substring(0, 7);
                    if (MainScreen.instance != null)
                        MainScreen.instance.refreshAfterAdd(ym);

                    recreate();
                })
                .setPositiveButton("EDIT", (d, w) -> {

                    AddExpenseDialog dlg = AddExpenseDialog.newInstance(e.id);
                    dlg.show(getSupportFragmentManager(), "EDIT_EXPENSE");
                })
                .show();
    }

    // ------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------
    private void addTotalRow(double total, String symbol) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(dp(12), dp(12), dp(12), dp(12));

        TextView label = new TextView(this);
        label.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1));
        label.setText("TOTAL");
        label.setTextSize(18);
        label.setTypeface(Typeface.DEFAULT_BOLD);
        label.setTextColor(0xFFB71C1C);

        TextView amt = new TextView(this);
        DecimalFormat df = new DecimalFormat("#,##0.00");
        String tf = df.format(total) + " " + symbol;

        SpannableString dsp = new SpannableString(tf);
        dsp.setSpan(new RelativeSizeSpan(0.85f),
                tf.length() - symbol.length(), tf.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        amt.setText(dsp);
        amt.setTextSize(18);
        amt.setTypeface(Typeface.DEFAULT_BOLD);
        amt.setTextColor(0xFFB71C1C);

        row.addView(label);
        row.addView(amt);
        expensesContainer.addView(row);
    }

    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }

    private Date parseWith(SimpleDateFormat fmt, String s) {
        try { fmt.setLenient(false); return fmt.parse(s); }
        catch (Exception e) { return null; }
    }

    private Date parseAny(String raw) {
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
        Calendar c = Calendar.getInstance();
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

    private String formatDisplay(String iso) {
        try {
            Date d = incoming.parse(iso);
            return display.format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    private String safeDisplay(String raw) {
        Date d = parseAny(raw);
        return (d != null) ? display.format(d) : raw;
    }
}
