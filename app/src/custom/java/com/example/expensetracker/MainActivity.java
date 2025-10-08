package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout btnAdd = findViewById(R.id.btnAdd);
        LinearLayout btnView = findViewById(R.id.btnView);
        ImageView btnGear = findViewById(R.id.gear_icon); // custom flavor gear

        if (btnAdd != null) {
            btnAdd.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, AddExpenseActivity.class))
            );
        }

        if (btnView != null) {
            btnView.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, ViewMenuActivity.class))
            );
        }

        if (btnGear != null) {
            btnGear.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class))
            );
        }
    }
}
