package com.pixelpen.whereitwent;

import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MonthGroup {

    public boolean isHeader = false;
    public String monthLabel = "";
    public boolean expanded = false;

    // List of inflated day-entry views
    public List<View> dayRows = new ArrayList<>();

    // NEW — required for totals
    public double monthTotal = 0;
    public String totalFormatted = "";

    // Constructor for normal month rows
    public MonthGroup(String label) {
        this.monthLabel = label;
    }

    // Constructor for header row
    public static MonthGroup makeHeader() {
        MonthGroup g = new MonthGroup("");
        g.isHeader = true;
        g.expanded = true;
        return g;
    }
}
