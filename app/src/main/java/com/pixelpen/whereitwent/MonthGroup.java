package com.pixelpen.whereitwent;

import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MonthGroup {

    // True only for the "Last 12 Months" header row
    public boolean isHeader = false;

    // Example: "Nov 2025"
    public String monthLabel = "";

    // NEW: total for the month
    public String total = "0.00 ₽";

    // Expanded/collapsed state
    public boolean expanded = false;

    // All child rows (day entries inflated from row_month_entry.xml)
    public List<View> dayRows = new ArrayList<>();

    // Constructor for a normal month row
    public MonthGroup(String label) {
        this.monthLabel = label;
    }

    // Constructor for header row
    public static MonthGroup makeHeader() {
        MonthGroup g = new MonthGroup("");
        g.isHeader = true;
        g.expanded = true;   // header never collapses
        return g;
    }
}
