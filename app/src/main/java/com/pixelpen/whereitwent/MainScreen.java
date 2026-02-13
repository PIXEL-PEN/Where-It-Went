package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainScreen extends AppCompatActivity {

    public static MainScreen instance;
    public static int expandMonthIndex = -1;

    private RecyclerView recyclerMonths;
    private RecyclerView.Adapter<?> adapter;

    boolean twelveMonthMode = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ExpenseDatabase.migrateAccountsFromPrefsIfNeeded(this);

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
        View linkAccounts = findViewById(R.id.linkAccounts);

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

        if (linkAccounts != null) {
            linkAccounts.setOnClickListener(v -> {
                startActivity(new Intent(MainScreen.this, AccountsOverviewActivity.class));
                drawerLayout.closeDrawer(Gravity.END);
            });
        }

        // Edit Categories (opens Manage Categories dialog)
        if (linkManageCategories != null) {
            linkManageCategories.setOnClickListener(v -> {
                drawerLayout.closeDrawer(Gravity.END);

                AddExpenseDialog dialog = new AddExpenseDialog();
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

        recyclerMonths = findViewById(R.id.recycler_months);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        List<MainRow> rows =
                MainBuilder.build(this, twelveMonthMode);

        adapter = new MainAdapter(rows);
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

        List<MainRow> rows =
                MainBuilder.build(this, twelveMonthMode);


        // restore month expansion
        int idx = expandMonthIndex;
        expandMonthIndex = -1;

        if (idx >= 0) {

            int monthCounter = 0;

            for (MainRow row : rows) {

                if (row instanceof MonthGroup) {

                    MonthGroup mg = (MonthGroup) row;

                    mg.expanded = (monthCounter == idx);

                    monthCounter++;
                }
            }
        }

        adapter = new MainAdapter(rows);
        recyclerMonths.setAdapter(adapter);

        if (idx >= 0) {
            recyclerMonths.post(() -> {
                LinearLayoutManager lm =
                        (LinearLayoutManager) recyclerMonths.getLayoutManager();
                if (lm != null) {
                    lm.scrollToPositionWithOffset(idx + 1, 0);
                }
            });
        }
    }

}
