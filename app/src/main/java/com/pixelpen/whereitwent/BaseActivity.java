package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.widget.ImageButton;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;

/**
 * BaseActivity
 * ------------------------------------------------------
 * Central superclass providing global right-side slider
 * behavior controlled by the current screen’s hamburger button.
 * Any Activity extending this class will automatically
 * inherit a working drawer system.
 */
public abstract class BaseActivity extends AppCompatActivity {

    protected DrawerLayout drawerLayout;
    protected ImageButton btnHamburger;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Note: actual layout is set in each subclass (via setContentView)
    }

    /**
     * Initialize the drawer components after setContentView() is called.
     * Safe to call from any Activity that includes a drawer layout.
     */
    protected void initDrawer() {
        drawerLayout = findViewById(R.id.drawer_layout);
        btnHamburger  = findViewById(R.id.btn_filter);

        if (drawerLayout == null || btnHamburger == null) {
            // Activity doesn't include a drawer; silently skip.
            return;
        }

        btnHamburger.setOnClickListener(v -> toggleDrawer());
    }

    /** Opens or closes the right-side drawer with animation */
    protected void toggleDrawer() {
        if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            drawerLayout.openDrawer(GravityCompat.END);
        }
    }

    /** Force-close drawer when back pressed */
    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
        } else {
            super.onBackPressed();
        }
    }
}
