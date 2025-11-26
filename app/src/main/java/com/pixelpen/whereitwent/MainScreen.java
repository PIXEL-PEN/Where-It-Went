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

    public static MainScreen instance;

    private RecyclerView recyclerMonths;
    private MonthAdapter adapter;
    private String currencySymbol;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

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

        // ------------------------------------------------------
        // LOAD SAVED CURRENCY SYMBOL (SettingsActivity stores this)
        // ------------------------------------------------------
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        currencySymbol = prefs.getString("currency_symbol", "$");

        recyclerMonths = findViewById(R.id.recycler_months);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        // Build Month View using correct symbol
        List<MonthGroup> data = MonthBuilder.buildLast12Months(this, currencySymbol);
        adapter = new MonthAdapter(data);
        recyclerMonths.setAdapter(adapter);

        // FAB → Add Expense dialog
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
        instance = null;
    }

    // ------------------------------------------------------
    // CALLED AFTER BOTH NEW + EDITED EXPENSES
    // ------------------------------------------------------
    public void refreshAfterAdd() {

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        currencySymbol = prefs.getString("currency_symbol", "$");

        List<MonthGroup> fresh = MonthBuilder.buildLast12Months(this, currencySymbol);
        adapter = new MonthAdapter(fresh);
        recyclerMonths.setAdapter(adapter);
    }
}
