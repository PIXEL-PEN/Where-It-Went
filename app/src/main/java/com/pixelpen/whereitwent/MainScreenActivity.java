package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainScreenActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        drawerLayout = findViewById(R.id.drawer_layout);

        View ham = findViewById(R.id.btn_filter);
        if (ham != null) {
            ham.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(Gravity.END);
                }
            });
        }

        ImageButton fab = findViewById(R.id.fab_add);
        if (fab != null) {
            fab.setOnClickListener(v -> {
                AddExpenseDialog dlg = new AddExpenseDialog();
                dlg.show(getSupportFragmentManager(), "ADD_EXPENSE");
            });
        }

    }
}
