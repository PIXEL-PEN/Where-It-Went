package com.pixelpen.whereitwent;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;


@Database(
        entities = {
                Expense.class,
                AccountEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class ExpenseDatabase extends RoomDatabase {

    public abstract ExpenseDao expenseDao();
    public abstract AccountDao accountDao();

    private static ExpenseDatabase INSTANCE;

    public static void migrateAccountsFromPrefsIfNeeded(Context context) {

        ExpenseDatabase db = getDatabase(context);
        AccountDao dao = db.accountDao();

        // If Room already has accounts, migration already ran
        if (dao.countAccounts() > 0) {
            return;
        }

        SharedPreferences prefs =
                context.getSharedPreferences(
                        "accounts_store",
                        Context.MODE_PRIVATE);

        String raw = prefs.getString("accounts", null);
        if (raw == null || raw.isEmpty()) {
            return;
        }

        try {
            JSONArray arr = new JSONArray(raw);

            for (int i = 0; i < arr.length(); i++) {
                JSONObject obj = arr.getJSONObject(i);
                String name = obj.getString("name");
                String type = obj.getString("type");

                dao.insert(new AccountEntity(name, type, false));
            }

        } catch (Exception ignored) {
        }
    }




    public static ExpenseDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ExpenseDatabase.class,
                            "expense_db"
                    )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .build();
        }
        return INSTANCE;
    }
}
