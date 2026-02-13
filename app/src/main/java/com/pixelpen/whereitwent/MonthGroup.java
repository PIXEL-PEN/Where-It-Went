package com.pixelpen.whereitwent;

import android.view.View;
import java.util.ArrayList;
import java.util.List;

public class MonthGroup implements MainRow {


    public boolean isHeader = false;
    public String monthLabel = "";
    public String isoMonth = "";
    public boolean expanded = false;

    public static class DayData {
        public String iso;
        public String monthAbbrev;
        public String dayNumber;
        public String description;
        public String category;
        public String amount;
    }

    public List<DayData> dayRows = new ArrayList<>();

    public double monthTotal = 0;
    public String totalFormatted = "";

    public MonthGroup(String label) {
        this.monthLabel = label;
    }

    public static MonthGroup makeHeader() {
        MonthGroup g = new MonthGroup("");
        g.isHeader = true;
        g.expanded = false;
        return g;
    }
}
