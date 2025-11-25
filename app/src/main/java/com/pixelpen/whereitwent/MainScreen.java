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

    // ⭐ Allow DayDetailActivity to request a refresh after edit
    public static MainScreen instance;

    private RecyclerView recyclerMonths;
    private MonthAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        // Register instance
        instance = this;

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

        // Currency from settings
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
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        instance = null;   // prevent memory leaks
    }

    // ⭐ Called for NEW and EDITED expenses
    public void refreshAfterAdd() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "USD");
        String symbol = CurrencyUtils.symbolFor(code);

        List<MonthGroup> fresh = MonthBuilder.buildLast12Months(this, symbol);
        adapter = new MonthAdapter(fresh);
        recyclerMonths.setAdapter(adapter);
    }
}
