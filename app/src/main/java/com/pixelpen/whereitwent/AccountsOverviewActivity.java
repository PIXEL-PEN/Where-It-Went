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

        // -------------------------
        // PROJECTS
        // -------------------------
        addSection(inflater, container, "PROJECTS");

        // Project header (bold, total on right)
        addProjectHeader(inflater, container, "Kitchen Renovation", "฿12,450");

        // Project items (collapsible)
        addProjectItem(
                inflater,
                container,
                "Jan 22",
                "MEALS OUT",
                "฿18.50",
                "Late dinner after site visit"
        );

        addProjectItem(
                inflater,
                container,
                "Jan 23",
                "MATERIALS",
                "฿3,200",
                "Tile adhesive and spacers"
        );

        // -------------------------
        // TRAVEL
        // -------------------------
        addSection(inflater, container, "TRAVEL");

        // -------------------------
        // CUSTOM
        // -------------------------
        addSection(inflater, container, "CUSTOM");
    }

    // ----------------------------------------------------
    // SECTION HEADER (PROJECTS / TRAVEL / CUSTOM)
    // ----------------------------------------------------
    private void addSection(LayoutInflater inflater,
                            LinearLayout container,
                            String title) {

        View v = inflater.inflate(R.layout.row_account_section_blue, container, false);
        TextView t = v.findViewById(R.id.text_title);
        t.setText(title);
        container.addView(v);
    }

    // ----------------------------------------------------
    // PROJECT HEADER (Kitchen Renovation — total only)
    // ----------------------------------------------------
    private void addProjectHeader(LayoutInflater inflater,
                                  LinearLayout container,
                                  String name,
                                  String total) {

        View v = inflater.inflate(R.layout.row_account_item, container, false);

        TextView nameView  = v.findViewById(R.id.text_account_name);
        TextView totalView = v.findViewById(R.id.text_account_total);

        nameView.setText(name);
        totalView.setText(total);

        container.addView(v);
    }

    // ----------------------------------------------------
    // PROJECT ITEM (expand / collapse)
    // ----------------------------------------------------
    private void addProjectItem(LayoutInflater inflater,
                                LinearLayout container,
                                String date,
                                String category,
                                String amount,
                                String note) {

        // Item header row
        View header = inflater.inflate(
                R.layout.row_account_item_header,
                container,
                false
        );

        TextView monthView    = header.findViewById(R.id.text_month);
        TextView dayView      = header.findViewById(R.id.text_day);
        TextView categoryView = header.findViewById(R.id.text_category);
        TextView amountView   = header.findViewById(R.id.text_amount);

        // Expecting date format like "Jan 22"
        String[] parts = date.split(" ");
        if (parts.length == 2) {
            monthView.setText(parts[0]);
            dayView.setText(parts[1]);
        }

        categoryView.setText(category);
        amountView.setText(amount);

        // Item note row
        View noteRow = inflater.inflate(
                R.layout.row_account_item_note,
                container,
                false
        );

        TextView noteView = noteRow.findViewById(R.id.text_note);
        noteView.setText(note);

        // Toggle behavior
        header.setOnClickListener(v ->
                noteRow.setVisibility(
                        noteRow.getVisibility() == View.GONE
                                ? View.VISIBLE
                                : View.GONE
                )
        );

        container.addView(header);
        container.addView(noteRow);
    }
}
