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
    public static int expandMonthIndex = -1;


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

        // Drawer links
        View linkSettings = findViewById(R.id.linkSettings);
        View linkCategories = findViewById(R.id.linkCategoryFilter);
        View linkManageCategories = findViewById(R.id.linkManageCategories);
        View linkDistribution = findViewById(R.id.linkDistribution);
        View linkTutorial = findViewById(R.id.linkTutorial);

        View linkPreview = findViewById(R.id.linkPreviewDialog);
        if (linkPreview != null) {
            linkPreview.setOnClickListener(v -> {
                showPreviewDialog();
                drawerLayout.closeDrawer(Gravity.END);
            });
        }



        if (linkSettings != null) {
            linkSettings.setOnClickListener(v -> {
                startActivity(new Intent(MainScreen.this, SettingsActivity.class));
                drawerLayout.closeDrawer(Gravity.END);
            });
        }

        if (linkCategories != null) {
            linkCategories.setOnClickListener(v -> {
                startActivity(new Intent(MainScreen.this, CategoryWiseActivity.class));
                drawerLayout.closeDrawer(Gravity.END);
            });
        }

        // Edit Categories (currently no Activity exists, so disable)
        if (linkManageCategories != null) {
            linkManageCategories.setOnClickListener(v -> {

                drawerLayout.closeDrawer(Gravity.END);

                // Create AddExpenseDialog instance
                AddExpenseDialog dialog = new AddExpenseDialog();

                // Mark that we want it to immediately open Manage Categories
                Bundle args = new Bundle();
                args.putBoolean("open_manage_categories", true);
                dialog.setArguments(args);

                dialog.show(getSupportFragmentManager(), "MANAGE_CATEGORIES");
            });
        }


        if (linkDistribution != null) {
            linkDistribution.setOnClickListener(v -> {
                startActivity(new Intent(MainScreen.this, DistributionActivity.class));
                drawerLayout.closeDrawer(Gravity.END);
            });
        }

        if (linkTutorial != null) {
            linkTutorial.setOnClickListener(v -> {
                startActivity(new Intent(MainScreen.this, TutorialActivity.class));
                drawerLayout.closeDrawer(Gravity.END);
            });
        }

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        currencySymbol = prefs.getString("currency_symbol", "$");

        recyclerMonths = findViewById(R.id.recycler_months);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        List<MonthGroup> data = MonthBuilder.buildLast12Months(this, currencySymbol);
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
        instance = null;
    }

    public void refreshAfterAdd() {

        // Reload currency
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        currencySymbol = prefs.getString("currency_symbol", "$");

        // Rebuild data
        List<MonthGroup> fresh = MonthBuilder.buildLast12Months(this, currencySymbol);

        // Determine which month should auto-expand
        int idx = expandMonthIndex;
        expandMonthIndex = -1;

        // Apply expand state
        for (int i = 0; i < fresh.size(); i++) {
            MonthGroup mg = fresh.get(i);

            if (!mg.isHeader && i == idx) {
                mg.expanded = true;    // auto-expand this month
            } else {
                mg.expanded = false;
            }
        }

        // Rebind adapter
        adapter = new MonthAdapter(fresh);
        recyclerMonths.setAdapter(adapter);

        // Optional: scroll into view
        if (idx >= 0) {
            recyclerMonths.post(() -> {
                LinearLayoutManager lm = (LinearLayoutManager) recyclerMonths.getLayoutManager();
                if (lm != null) {
                    lm.scrollToPositionWithOffset(idx, 0); // align item TOP
                }
            });
        }

    }

    private void showPreviewDialog() {
        androidx.appcompat.app.AlertDialog.Builder b =
                new androidx.appcompat.app.AlertDialog.Builder(this);

        View v = getLayoutInflater().inflate(R.layout.add_expense_dialog, null);
        b.setView(v);

        androidx.appcompat.app.AlertDialog dialog = b.create();
        dialog.show();
    }


}