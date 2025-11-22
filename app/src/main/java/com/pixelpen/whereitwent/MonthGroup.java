package com.pixelpen.whereitwent;

import java.util.ArrayList;
import java.util.List;

public class MonthGroup {

    public String monthLabel;      // "Nov 2025"
    public String isoMonth;        // "2025-11"
    public double total;           // sum of amounts
    public List<Expense> items;    // expenses for this month

    public boolean expanded = false;

    public MonthGroup() {
        items = new ArrayList<>();
    }
}
