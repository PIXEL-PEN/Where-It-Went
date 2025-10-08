package com.example.expensetracker;

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
}
