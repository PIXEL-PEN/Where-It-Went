package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import android.widget.Toast;

import java.util.List;

public class MainScreen extends AppCompatActivity {

    private RecyclerView recyclerMonths;
    private MonthAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main_screen);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);
        drawerLayout.setDrawerElevation(16f);
        drawerLayout.setScrimColor(0x55000000);

        View ham = findViewById(R.id.btn_filter);
        ham.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.END));

        recyclerMonths = findViewById(R.id.recycler_months);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        ImageButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setTranslationY(-90f);
        fabAdd.setOnClickListener(v -> {
            AddExpenseDialog dialog = new AddExpenseDialog();
            dialog.show(getSupportFragmentManager(), "ADD_EXPENSE");
        });

        List<MonthGroup> data = MonthBuilder.buildLast12Months(this);
        adapter = new MonthAdapter(this, data);
        recyclerMonths.setAdapter(adapter);
    }

    public void refreshAfterAdd() {
        List<MonthGroup> fresh = MonthBuilder.buildLast12Months(this);
        adapter = new MonthAdapter(this, fresh);
        recyclerMonths.setAdapter(adapter);
    }
}
