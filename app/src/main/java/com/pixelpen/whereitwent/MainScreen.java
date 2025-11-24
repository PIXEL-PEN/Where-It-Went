package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;

import java.util.List;

public class MainScreen extends AppCompatActivity {

    private RecyclerView recyclerMonths;
    private MonthAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        // Drawer
        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerElevation(16f);
        drawerLayout.setScrimColor(0x55000000);

        View ham = findViewById(R.id.btn_filter);
        ham.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.END));

        // Recycler setup
        recyclerMonths = findViewById(R.id.recycler_months);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        // FAB -> Add Expense Dialog
        ImageButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setTranslationY(-90f);
        fabAdd.setOnClickListener(v -> {
            AddExpenseDialog dialog = new AddExpenseDialog();
            dialog.show(getSupportFragmentManager(), "ADD_EXPENSE");
        });

        // Initial load
        List<MonthGroup> data = MonthBuilder.buildLast12Months(this);
        adapter = new MonthAdapter(this, data);
        recyclerMonths.setAdapter(adapter);
    }

    // Called from AddExpenseDialog after a successful save
    public void refreshAfterAdd() {

        // Rebuild month data
        List<MonthGroup> fresh = MonthBuilder.buildLast12Months(this);

        // Auto-expand the newest month (index 1)
        if (fresh.size() > 1) {
            fresh.get(1).expanded = true;
        }

        // Reload adapter
        adapter = new MonthAdapter(this, fresh);
        recyclerMonths.setAdapter(adapter);

        // Scroll so new entry is visible immediately
        recyclerMonths.scrollToPosition(1);
    }
}
