package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected ImageButton btnHamburger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    protected void initDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        btnHamburger = findViewById(R.id.btn_filter);

        if (btnHamburger != null) {
            btnHamburger.setOnClickListener(v -> toggleDrawer());
        }

        wireMenuItems();
    }

    private void wireMenuItems() {
        View linkSettings = findViewById(R.id.linkSettings);
        if (linkSettings != null) {
            linkSettings.setOnClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                closeDrawer();
            });
        }

        View linkCategoryFilter = findViewById(R.id.linkCategoryFilter);
        if (linkCategoryFilter != null) {
            linkCategoryFilter.setOnClickListener(v -> {
                Intent i = new Intent(this, CategoryWiseActivity.class);
                i.putExtra("show_filter_dialog", true);
                startActivity(i);
                closeDrawer();
            });
        }

        View linkDistribution = findViewById(R.id.linkDistribution);
        if (linkDistribution != null) {
            linkDistribution.setOnClickListener(v -> {
                startActivity(new Intent(this, DistributionActivity.class));
                closeDrawer();
            });
        }

        View linkTutorial = findViewById(R.id.linkTutorial);
        if (linkTutorial != null) {
            linkTutorial.setOnClickListener(v -> {
                startActivity(new Intent(this, TutorialActivity.class));
                closeDrawer();
            });
        }
    }

    protected void toggleDrawer() {
        if (drawerLayout == null) return;
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    protected void closeDrawer() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        }
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        super.onBackPressed();
    }
}
