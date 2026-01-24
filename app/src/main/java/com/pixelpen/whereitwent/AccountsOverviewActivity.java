package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AccountsOverviewActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_overview);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        LinearLayout container = findViewById(R.id.accounts_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        // PROJECTS (blue)
        addSectionBlue(inflater, container, "PROJECTS");
        addAccount(inflater, container, "Kitchen Renovation", "฿12,450");

        // TRAVEL (amber)
        addSectionAmber(inflater, container, "TRAVEL");

        // CUSTOM (blue)
        addSectionBlue(inflater, container, "CUSTOM");
    }

    private void addSectionBlue(LayoutInflater inflater,
                                LinearLayout container,
                                String title) {

        View v = inflater.inflate(R.layout.row_account_section_blue, container, false);
        TextView t = v.findViewById(R.id.text_title);
        t.setText(title);
        container.addView(v);
    }

    private void addSectionAmber(LayoutInflater inflater,
                                 LinearLayout container,
                                 String title) {

        View v = inflater.inflate(R.layout.row_account_section_amber, container, false);
        TextView t = v.findViewById(R.id.text_title);
        t.setText(title);
        container.addView(v);
    }

    private void addAccount(LayoutInflater inflater,
                            LinearLayout container,
                            String name,
                            String total) {

        View v = inflater.inflate(R.layout.row_account_item, container, false);

        TextView nameView = v.findViewById(R.id.text_account_name);
        TextView totalView = v.findViewById(R.id.text_account_total);

        nameView.setText(name);
        totalView.setText(total);

        container.addView(v);
    }
}
