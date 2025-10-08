package com.example.expensetracker;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

public class ViewMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_menu);

        findViewById(R.id.fab_add).setOnClickListener(v -> startActivity(new Intent(this, AddExpenseActivity.class)));


        AppCompatButton btnViewAll = findViewById(R.id.btnViewAll);
        AppCompatButton btnDateWise = findViewById(R.id.btnDateWise);
        AppCompatButton btnMonthWise = findViewById(R.id.btnMonthWise);
        AppCompatButton btnCategoryWise = findViewById(R.id.btnCategoryWise);

        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v -> {
                Intent intent = new Intent(ViewMenuActivity.this, ViewAllActivity.class);
                startActivity(intent);
            });
        }

        if (btnDateWise != null) {
            btnDateWise.setOnClickListener(v -> {
                Intent intent = new Intent(ViewMenuActivity.this, DateWiseActivity.class);
                startActivity(intent);
            });
        }

        if (btnMonthWise != null) {
            btnMonthWise.setOnClickListener(v -> {
                Intent intent = new Intent(ViewMenuActivity.this, MonthWiseActivity.class);
                startActivity(intent);
            });
        }

        if (btnCategoryWise != null) {
            btnCategoryWise.setOnClickListener(v -> {
                Intent intent = new Intent(ViewMenuActivity.this, CategoryWiseActivity.class);
                startActivity(intent);
            });
        }
    }
}
