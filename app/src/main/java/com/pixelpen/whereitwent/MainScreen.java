package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.content.Intent;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class MainScreen extends AppCompatActivity {

    private RecyclerView recyclerMonths;
    private MonthAdapterStub adapter;   // minimal placeholder adapter

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        // Drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setScrimColor(0x55000000);

        View ham = findViewById(R.id.btn_filter);
        ham.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.END));

        // Header
        TextView headerTitle = findViewById(R.id.text_header_12mo);
        TextView headerTotal = findViewById(R.id.text_header_total);

        headerTitle.setText("Last 12 Months");
        headerTotal.setText("0.00 ₱");

        // FAB
        ImageButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setTranslationY(-90f);
        fabAdd.setOnClickListener(v -> {
            AddExpenseDialog dialog = new AddExpenseDialog();
            dialog.show(getSupportFragmentManager(), "ADD_EXPENSE");
        });

        // RecyclerView setup
        recyclerMonths = findViewById(R.id.recycler_month_groups);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        adapter = new MonthAdapterStub();
        recyclerMonths.setAdapter(adapter);
    }

    // Called after adding an expense
    public void refreshAfterAdd() {
        // For now, Option A = no actual month logic
        adapter.notifyDataSetChanged();
    }
}
