package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import android.widget.ArrayAdapter;
import android.widget.Spinner;


public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_reorder);

        Spinner spinnerCategory = findViewById(R.id.spinner_category);
        if (spinnerCategory != null) {
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                    this,
                    R.array.category_names,
                    R.layout.spinner_item_selected
            );
            adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
            spinnerCategory.setAdapter(adapter);
            spinnerCategory.setSelection(0, false);
        }


        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_filter);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                View drawerView = findViewById(R.id.include_drawer);
                if (drawerLayout.isDrawerOpen(drawerView)) {
                    drawerLayout.closeDrawer(drawerView);
                } else {
                    drawerLayout.openDrawer(drawerView);
                }
            });
        }

        // Hook up slider drawer links only
        View drawer = findViewById(R.id.include_drawer);
        if (drawer != null) {
            // Category Filter → opens dialog directly
            View linkCategoryFilter = drawer.findViewById(R.id.linkCategoryFilter);
            if (linkCategoryFilter != null) {
                linkCategoryFilter.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, CategoryWiseActivity.class);
                    intent.putExtra("show_filter_dialog", true);
                    startActivity(intent);
                    drawerLayout.closeDrawers();
                });
            }

            // Edit Categories (new direct link)
            View linkManageCategories = drawer.findViewById(R.id.linkManageCategories);
            if (linkManageCategories != null) {
                linkManageCategories.setOnClickListener(v -> {
                    Intent intent = new Intent(MainActivity.this, AddExpenseActivity.class);
                    intent.putExtra("open_manager_direct", true);
                    startActivity(intent);
                    drawerLayout.closeDrawers();
                });
            }

            // Settings
            View linkSettings = drawer.findViewById(R.id.linkSettings);
            if (linkSettings != null) {
                linkSettings.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                    drawerLayout.closeDrawers();
                });
            }

            // Distribution (placeholder)
            View linkDistribution = drawer.findViewById(R.id.linkDistribution);
            if (linkDistribution != null) {
                linkDistribution.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, DistributionActivity.class));
                    drawerLayout.closeDrawers();
                });
            }

            // Tutorial (placeholder)
            View linkTutorial = drawer.findViewById(R.id.linkTutorial);
            if (linkTutorial != null) {
                linkTutorial.setOnClickListener(v -> {
                    startActivity(new Intent(MainActivity.this, TutorialActivity.class));
                    drawerLayout.closeDrawers();
                });
            }
        }

        // Main screen navigation buttons (restored)
        View btnViewAll = findViewById(R.id.btnViewAll);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v ->
                    startActivity(new Intent(this, ViewAllActivity.class))
            );
        }

        View btnDateWise = findViewById(R.id.btnDateWise);
        if (btnDateWise != null) {
            btnDateWise.setOnClickListener(v ->
                    startActivity(new Intent(this, DateWiseActivity.class))
            );
        }

        View btnMonthWise = findViewById(R.id.btnMonthWise);
        if (btnMonthWise != null) {
            btnMonthWise.setOnClickListener(v ->
                    startActivity(new Intent(this, MonthWiseActivity.class))
            );
        }

        View btnCategoryWise = findViewById(R.id.btnCategoryWise);
        if (btnCategoryWise != null) {
            btnCategoryWise.setOnClickListener(v ->
                    startActivity(new Intent(this, CategoryWiseActivity.class))
            );
        }

        // Add Expense FAB (restored)
        ImageButton fabAdd = findViewById(R.id.fab_add);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v ->
                    startActivity(new Intent(this, AddExpenseActivity.class))
            );
        }
    }
}
