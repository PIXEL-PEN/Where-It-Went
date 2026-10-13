package com.pixelpen.whereitwent;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Full backup and Import (v3.2).
 *
 * The calling activity must forward {@code onActivityResult} to
 * {@link #onActivityResult}, as for {@link SummaryExport}.
 */
public final class DataTransfer {

    public static final int REQ_SAVE_BACKUP = 3201;
    public static final int REQ_OPEN_IMPORT = 3202;

    private static final int MAX_FILE_BYTES = 20 * 1024 * 1024;

    private static String pendingBackup = null;

    private DataTransfer() { }

    // ========================================================
    // FULL BACKUP
    // ========================================================

    /** Builds the backup now and asks where to save it. */
    public static void startFullBackup(Activity activity) {
        try {
            pendingBackup = buildBackup(activity);
        } catch (Exception e) {
            Toast.makeText(activity, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return;
        }
        String day = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH).format(new Date());

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "WhereItWent-backup-" + day + ".csv");
        activity.startActivityForResult(intent, REQ_SAVE_BACKUP);
    }

    private static String buildBackup(Activity activity) {
        ExpenseDatabase db = ExpenseDatabase.getDatabase(activity);

        Map<String, String> categories = new LinkedHashMap<>();
        for (String name : CategoryManager.getOrderedCategories(activity)) {
            categories.put(name, CategoryManager.getTagForCategory(activity, name));
        }

        List<AccountEntity> accountEntities = db.accountDao().getAllAccountsById();
        Collections.reverse(accountEntities);                 // oldest first
        Map<Long, String> nameById = new HashMap<>();
        List<BackupFormat.AccountRec> accounts = new ArrayList<>();
        for (AccountEntity a : accountEntities) {
            nameById.put(a.id, a.name);
            accounts.add(new BackupFormat.AccountRec(a.name, a.type, a.archived));
        }

        List<Expense> expenseEntities = db.expenseDao().getAll();
        Collections.reverse(expenseEntities);                 // oldest first, as entered
        List<BackupFormat.ExpenseRec> expenses = new ArrayList<>();
        for (Expense e : expenseEntities) {
            String iso = DateUtils.toIso(e.date);
            expenses.add(new BackupFormat.ExpenseRec(iso != null ? iso : String.valueOf(e.date),
                    e.category, e.description == null ? "" : e.description, e.amount));
        }

        List<AccountItemEntity> itemEntities = db.accountItemDao().getAllItems();
        Collections.reverse(itemEntities);                    // oldest first
        List<BackupFormat.AccountItemRec> items = new ArrayList<>();
        SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        for (AccountItemEntity it : itemEntities) {
            String name = nameById.get(it.accountId);
            if (name == null) continue;   // left behind by an account deleted before v3.2
            String iso = DateUtils.toIso(it.date);
            if (iso == null && it.dateMillis > 0) iso = isoFmt.format(new Date(it.dateMillis));
            items.add(new BackupFormat.AccountItemRec(name, iso != null ? iso : String.valueOf(it.date),
                    it.item == null ? "" : it.item, it.category == null ? "" : it.category,
                    it.amount, it.note == null ? "" : it.note));
        }

        return BackupFormat.writeBackup(AppPrefs.getCurrencySymbol(activity),
                categories, accounts, expenses, items, new Date());
    }

    // ========================================================
    // IMPORT
    // ========================================================

    public static void startImport(Activity activity) {
        new AlertDialog.Builder(activity)
                .setTitle("Import")
                .setMessage("Choose a Where It Went full backup, or an \"All items\" CSV export "
                        + "(Daily Living or Accounts).\n\n"
                        + "You'll see what the file contains before anything is added. "
                        + "Entries already in the app are skipped, and nothing is deleted.")
                .setPositiveButton("Choose file", (d, w) -> {
                    Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                    intent.addCategory(Intent.CATEGORY_OPENABLE);
                    intent.setType("*/*");
                    activity.startActivityForResult(intent, REQ_OPEN_IMPORT);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private static void previewImport(Activity activity, Uri uri) {
        String text;
        try {
            text = readText(activity, uri);
        } catch (Exception e) {
            message(activity, "Couldn't read file", String.valueOf(e.getMessage()));
            return;
        }

        BackupFormat.Parsed parsed = BackupFormat.parse(text);

        if (parsed.kind == BackupFormat.Kind.SUMMARY_CSV) {
            message(activity, "Can't import this file",
                    "This is a Category Summary, which only holds totals. "
                            + "Choose a full backup or an \"All items\" CSV export instead.");
            return;
        }
        if (parsed.kind == BackupFormat.Kind.UNKNOWN) {
            message(activity, "Can't import this file",
                    "This isn't a Where It Went backup or CSV export. "
                            + "Email (HTML) exports can't be imported; use a CSV export or a full backup.");
            return;
        }

        ExpenseDatabase db = ExpenseDatabase.getDatabase(activity);
        String currency = AppPrefs.getCurrencySymbol(activity);
        ImportPlan plan = ImportPlan.build(parsed,
                db.expenseDao().getAll(),
                db.accountDao().getAllAccounts(),
                db.accountItemDao().getAllItems(),
                CategoryManager.getOrderedCategories(activity),
                currency,
                CategoryManager.TAG_FIXED);

        boolean nothingNew = plan.totalToAdd() == 0 && plan.accountsToCreate.isEmpty()
                && plan.categoriesToAdd.isEmpty() && plan.currencyToApply == null;

        AlertDialog.Builder b = new AlertDialog.Builder(activity)
                .setTitle(plan.title())
                .setMessage(plan.describe(currency));
        if (nothingNew) {
            b.setPositiveButton("OK", null);
        } else {
            b.setPositiveButton("Import", (d, w) -> applyImport(activity, plan))
             .setNegativeButton("Cancel", null);
        }
        b.show();
    }

    private static void applyImport(Activity activity, ImportPlan plan) {
        ExpenseDatabase db = ExpenseDatabase.getDatabase(activity);
        try {
            db.runInTransaction(() -> {
                AccountDao accountDao = db.accountDao();
                for (BackupFormat.AccountRec a : plan.accountsToCreate) {
                    accountDao.insert(new AccountEntity(a.name, a.type, a.archived));
                }

                Map<String, Long> idByName = new HashMap<>();
                for (AccountEntity a : accountDao.getAllAccounts()) {
                    idByName.put(a.name.trim().toLowerCase(Locale.ROOT), a.id);
                }

                ExpenseDao expenseDao = db.expenseDao();
                for (BackupFormat.ExpenseRec r : plan.expensesToAdd) {
                    Expense e = new Expense();
                    e.date = r.isoDate;
                    e.category = r.category;
                    e.description = r.item;
                    e.amount = r.amount;
                    expenseDao.insert(e);
                }

                AccountItemDao itemDao = db.accountItemDao();
                SimpleDateFormat isoFmt = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                SimpleDateFormat uiFmt = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
                for (BackupFormat.AccountItemRec r : plan.accountItemsToAdd) {
                    Long id = idByName.get(r.accountName.trim().toLowerCase(Locale.ROOT));
                    if (id == null) continue;
                    Date d;
                    try {
                        d = isoFmt.parse(r.isoDate);
                    } catch (Exception ex) {
                        continue;
                    }
                    // Same form as items added in the app: local midnight, "dd MMM yyyy"
                    Calendar c = Calendar.getInstance();
                    c.setTime(d);
                    c.set(Calendar.HOUR_OF_DAY, 0);
                    c.set(Calendar.MINUTE, 0);
                    c.set(Calendar.SECOND, 0);
                    c.set(Calendar.MILLISECOND, 0);
                    itemDao.insert(new AccountItemEntity(id, uiFmt.format(c.getTime()),
                            c.getTimeInMillis(), r.item, r.category, r.amount,
                            r.note == null || r.note.isEmpty() ? null : r.note));
                }
            });
        } catch (Exception e) {
            // The transaction rolls back, so nothing was added
            message(activity, "Import failed",
                    "Nothing was added.\n\n" + e.getMessage());
            return;
        }

        for (Map.Entry<String, String> c : plan.categoriesToAdd.entrySet()) {
            CategoryManager.saveCategoryWithTag(activity, c.getKey(), c.getValue());
        }
        if (plan.currencyToApply != null) {
            AppPrefs.setCurrencySymbol(activity, plan.currencyToApply);
        }

        Toast.makeText(activity,
                String.format(Locale.US, "Imported %,d entries", plan.totalToAdd()),
                Toast.LENGTH_LONG).show();
    }

    // ========================================================
    // RESULTS
    // ========================================================

    /** @return true if the result belonged to a backup or import and was handled. */
    public static boolean onActivityResult(Activity activity, int requestCode,
                                           int resultCode, Intent data) {
        if (requestCode == REQ_SAVE_BACKUP) {
            String content = pendingBackup;
            pendingBackup = null;
            if (resultCode != Activity.RESULT_OK || data == null || data.getData() == null) return true;
            if (content == null) {
                Toast.makeText(activity, "Backup failed: please try again", Toast.LENGTH_LONG).show();
                return true;
            }
            try (OutputStream os = activity.getContentResolver().openOutputStream(data.getData())) {
                if (os == null) throw new IllegalStateException("can't open file");
                os.write(content.getBytes(StandardCharsets.UTF_8));
                Toast.makeText(activity, "Full backup saved", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(activity, "Backup failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
            return true;
        }

        if (requestCode == REQ_OPEN_IMPORT) {
            if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
                previewImport(activity, data.getData());
            }
            return true;
        }
        return false;
    }

    // ========================================================
    // HELPERS
    // ========================================================

    private static String readText(Activity activity, Uri uri) throws Exception {
        try (InputStream in = activity.getContentResolver().openInputStream(uri)) {
            if (in == null) throw new IllegalStateException("can't open file");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            int n, total = 0;
            while ((n = in.read(buf)) > 0) {
                total += n;
                if (total > MAX_FILE_BYTES) throw new IllegalStateException("file is too large");
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    private static void message(Activity activity, String title, String text) {
        new AlertDialog.Builder(activity)
                .setTitle(title)
                .setMessage(text)
                .setPositiveButton("OK", null)
                .show();
    }
}
