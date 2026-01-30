package com.pixelpen.whereitwent;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

@Database(
        entities = {
                Expense.class,
                AccountEntity.class,
                AccountItemEntity.class
        },
        version = 5,
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

        if (prefs.getBoolean("accounts_items_remapped_v1", false)) {
            return;
        }

        String raw = prefs.getString("accounts", null);
        if (raw == null || raw.isEmpty()) {
            return;
        }

        try {
            JSONArray arr = new JSONArray(raw);

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

            List<AccountEntity> roomAccounts =
                    accountDao.getAllAccountsById();

            for (int i = 0; i < roomAccounts.size(); i++) {

                long legacyId = i + 1;
                long newRoomId = roomAccounts.get(i).id;

                List<AccountItemEntity> items =
                        itemDao.getItemsForAccount(legacyId);

                for (AccountItemEntity item : items) {
                    item.accountId = newRoomId;
                    itemDao.update(item);
                }
            }

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
                    .addMigrations(
                            MIGRATION_3_4,
                            MIGRATION_4_5
                    )
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
                            "dateMillis INTEGER NOT NULL DEFAULT 0, " +
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

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_account_items_dateMillis " +
                            "ON account_items(dateMillis)"
            );
        }
    };


    // ----------------------------------------------------
    // MIGRATION: v4 → v5 (ADD dateMillis)
    // ----------------------------------------------------
    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase db) {

            db.execSQL(
                    "ALTER TABLE account_items " +
                            "ADD COLUMN dateMillis INTEGER NOT NULL DEFAULT 0"
            );

            Cursor c = db.query(
                    "SELECT id, date FROM account_items"
            );

            SimpleDateFormat sdf =
                    new SimpleDateFormat(
                            "dd MMM yyyy",
                            Locale.ENGLISH
                    );

            while (c.moveToNext()) {
                long id = c.getLong(0);
                String date = c.getString(1);

                try {
                    long millis = sdf.parse(date).getTime();
                    db.execSQL(
                            "UPDATE account_items SET dateMillis = ? WHERE id = ?",
                            new Object[]{millis, id}
                    );
                } catch (Exception ignored) {
                }
            }

            c.close();

            db.execSQL(
                    "CREATE INDEX IF NOT EXISTS index_account_items_dateMillis " +
                            "ON account_items(dateMillis)"
            );
        }
    };
}
