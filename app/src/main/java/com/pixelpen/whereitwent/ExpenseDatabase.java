package com.pixelpen.whereitwent;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.List;

@Database(
        entities = {
                Expense.class,
                AccountEntity.class,
                AccountItemEntity.class
        },
        version = 4,
        exportSchema = false
)
public abstract class ExpenseDatabase extends RoomDatabase {

    public abstract ExpenseDao expenseDao();
    public abstract AccountDao accountDao();
    public abstract AccountItemDao accountItemDao();

    private static ExpenseDatabase INSTANCE;

    // ----------------------------------------------------
    // ONE-TIME PREFS → ROOM ACCOUNT + ITEM REMAP
    // ----------------------------------------------------
    public static void migrateAccountsFromPrefsIfNeeded(Context context) {

        ExpenseDatabase db = getDatabase(context);
        AccountDao accountDao = db.accountDao();
        AccountItemDao itemDao = db.accountItemDao();

        SharedPreferences prefs =
                context.getSharedPreferences(
                        "accounts_store",
                        Context.MODE_PRIVATE
                );

        // ------------------------------------------------
        // CORRECT GUARD (replaces countAccounts() check)
        // ------------------------------------------------
        if (prefs.getBoolean("accounts_items_remapped_v1", false)) {
            return;
        }

        String raw = prefs.getString("accounts", null);
        if (raw == null || raw.isEmpty()) {
            return;
        }

        try {
            JSONArray arr = new JSONArray(raw);

            // ---------------------------------------------
            // STEP 1: Ensure accounts exist (insert if empty)
            // ---------------------------------------------
            if (accountDao.countAccounts() == 0) {
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    String name = obj.getString("name");
                    String type = obj.getString("type");

                    accountDao.insert(
                            new AccountEntity(name, type, false)
                    );
                }
            }

            // ---------------------------------------------
            // STEP 2: Fetch accounts ordered by Room ID
            // ---------------------------------------------
            List<AccountEntity> roomAccounts =
                    accountDao.getAllAccountsById();

            // ---------------------------------------------
            // STEP 3: Remap account items by legacy ID
            // ---------------------------------------------
            for (int i = 0; i < roomAccounts.size(); i++) {

                long legacyId = i + 1;                 // old numeric ID
                long newRoomId = roomAccounts.get(i).id;

                List<AccountItemEntity> items =
                        itemDao.getItemsForAccount(legacyId);

                for (AccountItemEntity item : items) {
                    item.accountId = newRoomId;
                    itemDao.update(item);
                }
            }

            // ---------------------------------------------
            // STEP 4: Mark remap complete (ONE TIME)
            // ---------------------------------------------
            prefs.edit()
                    .putBoolean("accounts_items_remapped_v1", true)
                    .apply();

        } catch (Exception ignored) {
        }
    }

    // ----------------------------------------------------
    // DATABASE INSTANCE
    // ----------------------------------------------------
    public static ExpenseDatabase getDatabase(Context context) {
        if (INSTANCE == null) {
            INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            ExpenseDatabase.class,
                            "expense_db"
                    )
                    .addMigrations(MIGRATION_3_4)
                    .allowMainThreadQueries()
                    .build();
        }
        return INSTANCE;
    }

    // ----------------------------------------------------
    // MIGRATION: v3 → v4 (ADD account_items)
    // ----------------------------------------------------
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {

            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS account_items (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, " +
                            "accountId INTEGER NOT NULL, " +
                            "date TEXT, " +
                            "item TEXT, " +
                            "category TEXT, " +
                            "amount REAL NOT NULL, " +
                            "note TEXT" +
                            ")"
            );

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_account_items_accountId " +
                            "ON account_items(accountId)"
            );

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_account_items_date " +
                            "ON account_items(date)"
            );
        }
    };
}
