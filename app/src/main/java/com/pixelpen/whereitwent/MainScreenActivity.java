package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

public class MainScreenActivity extends AppCompatActivity {

    private DrawerLayout drawer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        drawer = findViewById(R.id.drawer_layout);

        View ham = findViewById(R.id.btn_filter);
        if (ham != null) {
            ham.setOnClickListener(v ->
                    drawer.openDrawer(Gravity.END));
        }

        ImageButton fab = findViewById(R.id.fab_add);
        fab.setOnClickListener(v -> openAddDialog());
    }

    private void openAddDialog() {
        new AddExpenseDialog()
                .show(getSupportFragmentManager(), "ADD_EXPENSE");
    }
}
