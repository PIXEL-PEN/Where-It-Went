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
        TextView legend = findViewById(R.id.legend);

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);
        DecimalFormat money = new DecimalFormat("#,##0.00");

        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        List<Expense> filtered = DateRangeCutoff.filterByMonths(this, all);

        double fixed = 0, basic = 0, disc = 0;
        for (Expense e : filtered) {
            String tag = CategoryManager.getTagForCategory(this, safe(e.category));
            if ("Fixed".equalsIgnoreCase(tag)) fixed += e.amount;
            else if ("Basic".equalsIgnoreCase(tag) || "Necessities".equalsIgnoreCase(tag)) basic += e.amount;
            else disc += e.amount;
        }

        pie.setValues((float) fixed, (float) basic, (float) disc);

        String text = String.format(Locale.ENGLISH,
                "Fixed: %s %s\nBasic: %s %s\nDiscretionary: %s %s",
                money.format(fixed), symbol,
                money.format(basic), symbol,
                money.format(disc),  symbol);
        legend.setText(text);
    }

    private String safe(String s) { return s == null ? "" : s.trim(); }
}
