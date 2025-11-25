package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;

import java.util.List;

public class MainScreen extends AppCompatActivity {

    private RecyclerView recyclerMonths;
    private MonthAdapter adapter;

    // ★ NEW: flag so we expand after AddExpense succeeds
    private boolean shouldAutoExpand = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerElevation(16f);
        drawerLayout.setScrimColor(0x55000000);

        View ham = findViewById(R.id.btn_filter);
        if (ham != null) {
            ham.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.END));
        }

        View linkSettings = findViewById(R.id.linkSettings);
        if (linkSettings != null) {
            linkSettings.setOnClickListener(v -> {
                startActivity(new Intent(MainScreen.this, SettingsActivity.class));
                drawerLayout.closeDrawer(Gravity.END);
            });
        }

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "USD");
        String symbol = CurrencyUtils.symbolFor(code);

        recyclerMonths = findViewById(R.id.recycler_months);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        List<MonthGroup> data = MonthBuilder.buildLast12Months(this, symbol);
        adapter = new MonthAdapter(data);
        recyclerMonths.setAdapter(adapter);

        ImageButton fabAdd = findViewById(R.id.fab_add);
        if (fabAdd != null) {
            fabAdd.setTranslationY(-90f);
            fabAdd.setOnClickListener(v -> {
                AddExpenseDialog dialog = new AddExpenseDialog();
                dialog.show(getSupportFragmentManager(), "ADD_EXPENSE");

                // ★ Mark that we should auto-expand on resume
                shouldAutoExpand = true;
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (shouldAutoExpand) {
            shouldAutoExpand = false;
            refreshAfterAdd(true);
        }
    }

    // Original method (legacy callers)
    public void refreshAfterAdd() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "USD");
        String symbol = CurrencyUtils.symbolFor(code);

        List<MonthGroup> fresh = MonthBuilder.buildLast12Months(this, symbol);

        // ★ EXPAND NEWEST MONTH (item after header)
        if (fresh.size() > 1) {
            fresh.get(1).expanded = true;
        }

        adapter = new MonthAdapter(fresh);
        recyclerMonths.setAdapter(adapter);
    }


    // Updated internal version
    private void refreshAfterAdd(boolean expandFirstMonth) {

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "USD");
        String symbol = CurrencyUtils.symbolFor(code);

        List<MonthGroup> fresh = MonthBuilder.buildLast12Months(this, symbol);

        // ★ EXPAND NEWEST MONTH (index 0)
        if (expandFirstMonth && fresh.size() > 1) {

            for (int i = 0; i < fresh.size(); i++) {
                if (!fresh.get(i).isHeader) {
                    fresh.get(i).expanded = (i == 1);
                    // WHY index 1?
                    // Because index 0 is the static header ("Last 12 Months")
                    break;
                }
            }
        }

        adapter = new MonthAdapter(fresh);
        recyclerMonths.setAdapter(adapter);
    }
}
