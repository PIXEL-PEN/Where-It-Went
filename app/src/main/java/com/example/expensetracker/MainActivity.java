package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        LinearLayout btnAdd = findViewById(R.id.btnAdd);
        LinearLayout btnView = findViewById(R.id.btnView);
        LinearLayout btnGear = findViewById(R.id.btnGear); // gear icon container

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
            // Short press → open Settings
            btnGear.setOnClickListener(v ->
                    startActivity(new Intent(MainActivity.this, SettingsActivity.class))
            );
        }
    }
}
