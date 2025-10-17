package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnFilter, fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Drawer setup
        drawerLayout = findViewById(R.id.drawer_layout);
        btnFilter = findViewById(R.id.btn_filter);

        if (btnFilter != null && drawerLayout != null) {
            btnFilter.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.END));
        }

        // Floating Add button → opens AddExpenseActivity
        fabAdd = findViewById(R.id.fab_add);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, AddExpenseActivity.class)));
        }

        // Main buttons navigation
        linkButton(R.id.btnViewAll, ViewAllActivity.class);
        linkButton(R.id.btnDateWise, DateWiseActivity.class);
        linkButton(R.id.btnMonthWise, MonthWiseActivity.class);
        linkButton(R.id.btnCategoryWise, CategoryWiseActivity.class);
    }

    private void linkButton(int id, Class<?> target) {
        View btn = findViewById(id);
        if (btn != null) {
            btn.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, target)));
        }
    }
}
