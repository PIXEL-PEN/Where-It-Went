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

        // =========================
        // PROJECTS
        // =========================
        addSection(inflater, container, "PROJECTS");

        // Project header (collapsible controller)
        View projectHeader = addProjectHeader(
                inflater,
                container,
                "Kitchen Renovation",
                "฿12,450"
        );

        // Container for project items
        LinearLayout projectItems = new LinearLayout(this);
        projectItems.setOrientation(LinearLayout.VERTICAL);
        projectItems.setVisibility(View.VISIBLE);
        container.addView(projectItems);

        // Collapse / expand entire project
        projectHeader.setOnClickListener(v -> {
            projectItems.setVisibility(
                    projectItems.getVisibility() == View.VISIBLE
                            ? View.GONE
                            : View.VISIBLE
            );
        });

        // Project items
        addProjectItem(
                inflater,
                projectItems,
                "Jan 22",
                "hammer",
                "MEALS OUT",
                "฿18.50",
                "Late dinner after site visit"
        );

        addProjectItem(
                inflater,
                projectItems,
                "Jan 22",
                "Hammer",
                "MEALS OUT",
                "฿18.50",
                "Late dinner after site visit"
        );


        // =========================
        // TRAVEL
        // =========================
        addSection(inflater, container, "TRAVEL");

        // =========================
        // CUSTOM
        // =========================
        addSection(inflater, container, "CUSTOM");
    }

    // ----------------------------------------------------
    // SECTION HEADER (PROJECTS / TRAVEL / CUSTOM)
    // ----------------------------------------------------
    private void addSection(LayoutInflater inflater,
                            LinearLayout container,
                            String title) {

        int layoutRes =
                title.equals("TRAVEL")
                        ? R.layout.row_account_section_amber
                        : R.layout.row_account_section_blue;

        View v = inflater.inflate(layoutRes, container, false);
        TextView t = v.findViewById(R.id.text_title);
        t.setText(title);
        container.addView(v);
    }

    // ----------------------------------------------------
    // PROJECT HEADER (bold, total on right)
    // ----------------------------------------------------
    private View addProjectHeader(LayoutInflater inflater,
                                  LinearLayout container,
                                  String name,
                                  String total) {

        View v = inflater.inflate(R.layout.row_account_item, container, false);

        TextView nameView  = v.findViewById(R.id.text_account_name);
        TextView totalView = v.findViewById(R.id.text_account_total);

        nameView.setText(name);
        totalView.setText(total);

        container.addView(v);
        return v;
    }

    // ----------------------------------------------------
    // PROJECT ITEM (no collapse here)
    // ----------------------------------------------------
    private void addProjectItem(LayoutInflater inflater,
                                LinearLayout container,
                                String date,
                                String item,
                                String category,
                                String amount,
                                String note) {

        // Header row
        View header = inflater.inflate(
                R.layout.row_account_item_header,
                container,
                false
        );

        TextView monthView    = header.findViewById(R.id.text_month);
        TextView dayView      = header.findViewById(R.id.text_day);
        TextView itemView     = header.findViewById(R.id.text_item);
        TextView categoryView = header.findViewById(R.id.text_category);
        TextView amountView   = header.findViewById(R.id.text_amount);

        // Date (expects "Jan 22")
        String[] parts = date.split(" ");
        if (parts.length == 2) {
            monthView.setText(parts[0]);
            dayView.setText(parts[1]);
        }

        // Content
        itemView.setText(item);          // e.g. "hammer"
        categoryView.setText(category);  // e.g. "MEALS OUT"
        amountView.setText(amount);

        // Note row (present but not visible yet)
        View noteRow = inflater.inflate(
                R.layout.row_account_item_note,
                container,
                false
        );

        TextView noteView = noteRow.findViewById(R.id.text_note);
        noteView.setText(note);
        noteRow.setVisibility(View.GONE);

        container.addView(header);
        container.addView(noteRow);
    }
}
