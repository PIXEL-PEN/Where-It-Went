package com.pixelpen.whereitwent;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "expenses")
public class Expense {
    @PrimaryKey(autoGenerate = true)
    public int id;

    public String category;
    public String description;
    public double amount;
    public String date; // e.g., "Wed. September 14 2025"

    // Default empty constructor required by Room
    public Expense() {}

    // Convenience constructor for inserts
    public Expense(String category, String description, double amount, String date) {
        this.category = category;
        this.description = description;
        this.amount = amount;
        this.date = date;
    }
}
