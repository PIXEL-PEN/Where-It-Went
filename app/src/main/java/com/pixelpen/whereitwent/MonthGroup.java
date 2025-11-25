package com.pixelpen.whereitwent;

import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MonthGroup {

    public boolean isHeader = false;
    public String monthLabel = "";
    public boolean expanded = false;   // <-- default collapsed
    public static class DayData {
        public String iso;
        public String monthAbbrev;
        public String dayNumber;
        public String description;
        public String category;
        public String amount;   // formatted
    }

    public List<DayData> dayRows = new ArrayList<>();

    // Totals (computed by MonthBuilder)
    public double monthTotal = 0;
    public String totalFormatted = "";

    public MonthGroup(String label) {
        this.monthLabel = label;
    }

    public static MonthGroup makeHeader() {
        MonthGroup g = new MonthGroup("");
        g.isHeader = true;
        g.expanded = false;   // <-- IMPORTANT: header collapsed on start
        return g;
    }
}
