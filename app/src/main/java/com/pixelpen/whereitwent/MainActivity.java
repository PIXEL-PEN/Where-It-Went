package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnMenu;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        btnMenu = findViewById(R.id.btn_filter);

        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout.isDrawerOpen(findViewById(R.id.include_drawer))) {
                    drawerLayout.closeDrawer(findViewById(R.id.include_drawer));
                } else {
                    drawerLayout.openDrawer(findViewById(R.id.include_drawer));
                }
            });
        }

        // Wire up drawer links
        setupDrawerLinks();

        // Existing navigation buttons
        findViewById(R.id.btnViewAll).setOnClickListener(v ->
                startActivity(new Intent(this, ViewAllActivity.class)));

        findViewById(R.id.btnDateWise).setOnClickListener(v ->
                startActivity(new Intent(this, DateWiseActivity.class)));

        findViewById(R.id.btnMonthWise).setOnClickListener(v ->
                startActivity(new Intent(this, MonthWiseActivity.class)));

        findViewById(R.id.btnCategoryWise).setOnClickListener(v ->
                startActivity(new Intent(this, CategoryWiseActivity.class)));

        findViewById(R.id.fab_add).setOnClickListener(v ->
                startActivity(new Intent(this, AddExpenseActivity.class)));
    }

    private void setupDrawerLinks() {
        // Settings
        TextView linkSettings = findViewById(R.id.linkSettings);
        if (linkSettings != null) {
            linkSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                drawerLayout.closeDrawers();
            });
        }

        // Category Filter — optional shortcut to Category screen
        TextView linkCategoryFilter = findViewById(R.id.linkCategoryFilter);
        if (linkCategoryFilter != null) {
            linkCategoryFilter.setOnClickListener(v -> {
                startActivity(new Intent(this, CategoryWiseActivity.class));
                drawerLayout.closeDrawers();
            });
        }

        // Distribution placeholder
        TextView linkDistribution = findViewById(R.id.linkDistribution);
        if (linkDistribution != null) {
            linkDistribution.setOnClickListener(v -> {
                startActivity(new Intent(this, DistributionActivity.class));
                drawerLayout.closeDrawers();
            });
        }

        // Tutorial placeholder
        TextView linkTutorial = findViewById(R.id.linkTutorial);
        if (linkTutorial != null) {
            linkTutorial.setOnClickListener(v -> {
                startActivity(new Intent(this, TutorialActivity.class));
                drawerLayout.closeDrawers();
            });
        }
    }
}
