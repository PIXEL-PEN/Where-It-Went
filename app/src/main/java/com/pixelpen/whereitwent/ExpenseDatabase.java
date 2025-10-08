package com.pixelpen.whereitwent;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {Expense.class}, version = 2, exportSchema = false)  // bumped to v2
public abstract class ExpenseDatabase extends RoomDatabase {
    public abstract ExpenseDao expenseDao();

    private static ExpenseDatabase INSTANCE;

    public static ExpenseDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            ExpenseDatabase.class, "expense_db")
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()   // ✅ recreate DB if schema changes
                    .build();
        }
        return INSTANCE;
    }
}
