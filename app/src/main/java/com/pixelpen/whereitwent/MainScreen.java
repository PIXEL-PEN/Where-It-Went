package com.pixelpen.whereitwent;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.Gravity;
import java.util.*;

public class MainScreen extends AppCompatActivity {

    private RecyclerView recyclerMonths;
    private MonthAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_screen);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.setDrawerElevation(16f);
        drawer.setScrimColor(0x55000000);

        View btnHam = findViewById(R.id.btn_filter);
        btnHam.setOnClickListener(v -> drawer.openDrawer(Gravity.END));

        ImageButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setTranslationY(-90f);
        fabAdd.setOnClickListener(v -> {
            AddExpenseDialog dialog = new AddExpenseDialog();
            dialog.show(getSupportFragmentManager(), "ADD_EXPENSE");
        });

        recyclerMonths = findViewById(R.id.recycler_months);
        recyclerMonths.setLayoutManager(new LinearLayoutManager(this));

        loadMonths();
    }

    private void loadMonths() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String currencySymbol = prefs.getString("currency_symbol", "$");

        List<MonthGroup> data = MonthBuilder.buildLast12Months(this, currencySymbol);

        expandCurrentMonth(data);   // expand the active month

        adapter = new MonthAdapter(data, currencySymbol);
        recyclerMonths.setAdapter(adapter);

        setupDrawerLinks();
    }

    private void expandCurrentMonth(List<MonthGroup> groups) {
        String currentLabel = new java.text.SimpleDateFormat("MMM yyyy", java.util.Locale.ENGLISH)
                .format(new java.util.Date());

        for (MonthGroup g : groups) {
            if (!g.isHeader && g.monthLabel.equals(currentLabel)) {
                g.expanded = true;
                break;
            }
        }
    }

    public void refreshAfterAdd() {
        loadMonths();   // reload everything with new values
    }

    private void setupDrawerLinks() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);

        findViewById(R.id.linkSettings).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, SettingsActivity.class));
            drawer.closeDrawer(Gravity.END);
        });
    }
}
