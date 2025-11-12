package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class DistributionActivity extends AppCompatActivity {

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

        // New legend views (match activity_distribution.xml)
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
        List<Expense> filtered = DateRangeCutoff.filterByMonths(this, all);

        double fixed = 0, basic = 0, disc = 0;
        for (Expense e : filtered) {
            String tag = CategoryManager.getTagForCategory(this, safe(e.category));
            if ("Fixed".equalsIgnoreCase(tag)) {
                fixed += e.amount;
            } else if ("Basic".equalsIgnoreCase(tag) || "Necessities".equalsIgnoreCase(tag)) {
                basic += e.amount;
            } else {
                disc += e.amount;
            }
        }

        pie.setValues((float) fixed, (float) basic, (float) disc);

        // Totals
        if (fixedTotal != null) fixedTotal.setText(String.format(Locale.ENGLISH, "%s %s", money.format(fixed), symbol));
        if (basicTotal != null) basicTotal.setText(String.format(Locale.ENGLISH, "%s %s", money.format(basic), symbol));
        if (discTotal  != null) discTotal.setText(String.format(Locale.ENGLISH, "%s %s", money.format(disc),  symbol));

        // Placeholders for deltas (to be computed when we add previous-period comparison)
        if (fixedDelta != null) fixedDelta.setText("—");
        if (basicDelta != null) basicDelta.setText("—");
        if (discDelta  != null) discDelta.setText("—");
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
