package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DateWiseActivity extends AppCompatActivity {

    private ExpenseAdapter adapter;

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

        RecyclerView recyclerView = findViewById(R.id.recycler_expenses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Load expenses sorted by date
        List<Expense> expenses = ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        Collections.sort(expenses, (e1, e2) -> {
            Date d1 = parseDate(e1.date);
            Date d2 = parseDate(e2.date);
            if (d1 != null && d2 != null) {
                int cmp = d2.compareTo(d1);
                if (cmp != 0) return cmp;
            } else if (d1 == null && d2 == null) {
                // continue
            } else {
                return (d1 == null) ? 1 : -1;
            }
            return Integer.compare(e2.id, e1.id);
        });

        adapter = new ExpenseAdapter(expenses);
        recyclerView.setAdapter(adapter);
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        // Footer total
        double total = 0.0;
        for (Expense e : expenses) total += e.amount;

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        DecimalFormat df = new DecimalFormat("#,##0.00");
        String formattedTotal = df.format(total) + " " + symbol;

        SpannableString totalDisplay = new SpannableString(formattedTotal);
        int start = formattedTotal.length() - symbol.length();
        totalDisplay.setSpan(new RelativeSizeSpan(0.85f), start, formattedTotal.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView tvTotalAmount = findViewById(R.id.tvTotalAmount);
        tvTotalAmount.setText(totalDisplay);
    }

    // 🔁 Ensure dialog changes reflect immediately after returning
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (adapter != null) adapter.handleActivityResult(this, requestCode, resultCode);
    }

    private Date parseDate(String raw) {
        String[] patterns = {"yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd MMM yyyy", "dd MMM. yyyy"};
        for (String p : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(p, Locale.ENGLISH);
                in.setLenient(false);
                return in.parse(raw);
            } catch (Exception ignore) {}
        }
        return null;
    }
}
