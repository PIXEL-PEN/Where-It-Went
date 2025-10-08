package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageButton;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;

/**
 * Main launcher screen — formerly ViewMenuActivity.
 * Displays navigation buttons for all expense views and the Add FAB.
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // View All
        AppCompatButton btnViewAll = findViewById(R.id.btnViewAll);
        if (btnViewAll != null) {
            btnViewAll.setOnClickListener(v ->
                    startActivity(new Intent(this, ViewAllActivity.class))
            );
        }

        // Date-wise
        AppCompatButton btnDateWise = findViewById(R.id.btnDateWise);
        if (btnDateWise != null) {
            btnDateWise.setOnClickListener(v ->
                    startActivity(new Intent(this, DateWiseActivity.class))
            );
        }

        // Month-wise
        AppCompatButton btnMonthWise = findViewById(R.id.btnMonthWise);
        if (btnMonthWise != null) {
            btnMonthWise.setOnClickListener(v ->
                    startActivity(new Intent(this, MonthWiseActivity.class))
            );
        }

        // Category-wise
        AppCompatButton btnCategoryWise = findViewById(R.id.btnCategoryWise);
        if (btnCategoryWise != null) {
            btnCategoryWise.setOnClickListener(v ->
                    startActivity(new Intent(this, CategoryWiseActivity.class))
            );
        }

        // Floating Add Expense button
        ImageButton fabAdd = findViewById(R.id.fab_add);
        if (fabAdd != null) {
            fabAdd.setOnClickListener(v ->
                    startActivity(new Intent(this, AddExpenseActivity.class))
            );
        }
    }
}
