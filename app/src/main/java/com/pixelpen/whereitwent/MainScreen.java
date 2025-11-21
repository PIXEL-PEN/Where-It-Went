package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import androidx.drawerlayout.widget.DrawerLayout;
import android.view.Gravity;
import android.content.Intent;




public class MainScreen extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Toast.makeText(this, "MAINSCREEN LOADED", Toast.LENGTH_LONG).show();

        setContentView(R.layout.activity_main_screen);

        DrawerLayout drawerLayout = findViewById(R.id.drawer_layout);

        drawerLayout.setDrawerElevation(16f);
        drawerLayout.setScrimColor(0x55000000);


        View ham = findViewById(R.id.btn_filter);
        ham.setOnClickListener(v -> drawerLayout.openDrawer(Gravity.END));



        ImageButton btnPrev = findViewById(R.id.btn_prev_month);
        ImageButton btnNext = findViewById(R.id.btn_next_month);
        ImageButton fabAdd = findViewById(R.id.fab_add);
        fabAdd.setTranslationY(-90f);



        btnPrev.setOnClickListener(v -> {});
        btnNext.setOnClickListener(v -> {});

        fabAdd.setOnClickListener(v -> {
            AddExpenseDialog dialog = new AddExpenseDialog();
            dialog.show(getSupportFragmentManager(), "ADD_EXPENSE");
        });

        buildMonthList();
    }

    private void buildMonthList() {
        LinearLayout monthList = findViewById(R.id.month_list);
        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 1; i <= 20; i++) {

            View row = inflater.inflate(R.layout.row_month_entry, monthList, false);

            TextView monthAbbrev = row.findViewById(R.id.text_month_abbrev);
            TextView dayNum = row.findViewById(R.id.text_day_number);
            TextView item = row.findViewById(R.id.text_item);
            TextView category = row.findViewById(R.id.text_category);
            TextView amount = row.findViewById(R.id.text_amount);

            int finalI = i;  // Or use real date values later

            row.setOnClickListener(v -> {
                Intent intent = new Intent(MainScreen.this, DayDetailActivity.class);
                intent.putExtra("day", finalI);
                intent.putExtra("month", "Nov");    // placeholder
                intent.putExtra("year", 2025);      // placeholder
                startActivity(intent);
            });



            monthAbbrev.setText("Nov");
            dayNum.setText(String.format("%02d", i));
            item.setText("Sample Item " + i);
            category.setText("CATEGORY");     // enforce CAPS
            amount.setText("100.00");

            monthList.addView(row);

            View divider = new View(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    1
            );
            lp.setMargins(0, 0, 0, 0);
            divider.setLayoutParams(lp);
            divider.setBackgroundColor(0xFF555555);

            monthList.addView(divider);


        }

    }
}
