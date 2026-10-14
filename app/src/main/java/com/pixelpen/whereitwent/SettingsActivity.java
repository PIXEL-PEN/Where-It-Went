package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileWriter;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Date;




public class SettingsActivity extends AppCompatActivity {

    private Spinner spinnerCurrency;
    private static final int CREATE_CSV_FILE = 2001;
    private String csvContent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Top forward button
        ImageButton btnForward = findViewById(R.id.btn_forward);
        if (btnForward != null) {
            btnForward.setOnClickListener(v -> {
                startActivity(new Intent(this, MainScreen.class));
                finish();
            });
        }


// About text
        TextView about = findViewById(R.id.text_about);
        if (about != null) {

            // --- VersionName ---
            String versionName;
            try {
                versionName = getPackageManager()
                        .getPackageInfo(getPackageName(), 0).versionName;
            } catch (Exception e) {
                versionName = "?";
            }

            String cleanVersion = versionName.split("-")[0];

            // --- Correct variant detection using packageName + debuggable flag ---
            String pkgName = getPackageName().toLowerCase(Locale.ROOT);

            String flavor;
            if (pkgName.endsWith(".dev")) {
                flavor = "dev";
            } else if (pkgName.endsWith(".stable")) {
                flavor = "stable";
            } else {
                flavor = "unknown";
            }

            boolean isDebuggable =
                    (getApplicationInfo().flags & android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0;

            String buildType = isDebuggable ? "debug" : "release";

            String variant;
            if (flavor.equals("dev") && buildType.equals("debug")) {
                variant = "dev";          // devDebug
            } else if (flavor.equals("stable") && buildType.equals("debug")) {
                variant = "debug";        // stableDebug
            } else if (flavor.equals("stable") && buildType.equals("release")) {
                variant = "release";      // stableRelease
            } else {
                variant = buildType;      // fallback
            }
            String packageId = getPackageName();



            // --- Header ---
            String header =
                    "<b>Where It Went v." + cleanVersion + "</b> (" + variant + ")<br/>" +
                            "<small>Package: " + getPackageName() + "</small><br/><br/>";


            // --- Full HTML ---
            String fullHtml = header + getString(R.string.about_text)
                    .replace("{SUBJECT}", Uri.encode("Where It Went " + cleanVersion + " feedback"));

            about.setText(Html.fromHtml(fullHtml, Html.FROM_HTML_MODE_LEGACY));
            about.setMovementMethod(LinkMovementMethod.getInstance());
        }


        // -------------------------------
        // CURRENCY SPINNER (Symbol mode)
        // -------------------------------
        spinnerCurrency = findViewById(R.id.spinner_currency);

        // v3.3: the full world currency list (the original 13 first)
        final List<String> currencies = new java.util.ArrayList<>();
        for (TripCurrency.Currency c : TripCurrency.ALL) {
            currencies.add(c.label());
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_selected,
                currencies
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCurrency.setAdapter(adapter);

        // Restore by code (settings from before v3.3 had only a symbol;
        // getCurrencyCode works the code out from it)
        int restoredIndex = Math.max(0, TripCurrency.indexOf(AppPrefs.getCurrencyCode(this)));
        spinnerCurrency.setSelection(restoredIndex, false);

        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                AppPrefs.setCurrency(SettingsActivity.this,
                        TripCurrency.ALL.get(position).code);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // ----------------------------------------------------
        // EXPORT BUTTON
        // ----------------------------------------------------
        findViewById(R.id.btn_export).setOnClickListener(v -> {
            String[] options = {
                    "Full backup (for restoring)",
                    "All items – Storage (CSV)",
                    "All items – Email (HTML)",
                    "12-month category summary"
            };
            new AlertDialog.Builder(this)
                    .setTitle("Export Data")
                    .setItems(options, (d, which) -> {
                        if (which == 0) DataTransfer.startFullBackup(this);
                        else if (which == 1) exportToStorage();
                        else if (which == 2) exportAndEmail();
                        else chooseSummaryPeriod();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // ----------------------------------------------------
        // IMPORT BUTTON (v3.2)
        // ----------------------------------------------------
        findViewById(R.id.btn_import).setOnClickListener(v -> DataTransfer.startImport(this));

        // ----------------------------------------------------
        // RESET BUTTON
        // ----------------------------------------------------
        findViewById(R.id.btn_reset).setOnClickListener(v -> chooseReset());
    }

    // =====================================================
    // RESET (v3.2: choose Daily Living, Accounts or both)
    // =====================================================
    private static final int RESET_DAILY = 0, RESET_ACCOUNTS = 1, RESET_ALL = 2;

    private void chooseReset() {
        String[] options = {
                "Daily Living entries",
                "Accounts (all accounts and their entries)",
                "Everything (Daily Living and Accounts)"
        };
        new AlertDialog.Builder(this)
                .setTitle("Reset: what should be deleted?")
                .setItems(options, (d, which) -> confirmReset(which, options[which]))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmReset(int what, String label) {
        new AlertDialog.Builder(this)
                .setTitle("Delete " + label.split(" \\(")[0] + "?")
                .setMessage("This deletes: " + label + ".\nIt cannot be undone. "
                        + "Categories and settings are kept.\n\n"
                        + "Tip: make a Full backup first, so you can restore with Import.")
                .setPositiveButton("Delete", (d, w) -> doReset(what))
                .setNeutralButton("Back up first", (d, w) -> DataTransfer.startFullBackup(this))
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void doReset(int what) {
        ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
        db.runInTransaction(() -> {
            if (what == RESET_DAILY || what == RESET_ALL) {
                db.expenseDao().clearAll();
            }
            if (what == RESET_ACCOUNTS || what == RESET_ALL) {
                db.accountItemDao().clearAll();
                db.accountDao().clearAll();
            }
        });
        String msg = what == RESET_DAILY ? "Daily Living entries deleted"
                : what == RESET_ACCOUNTS ? "Accounts deleted"
                : "All entries deleted";
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private String fmt(String iso) {
        try {
            SimpleDateFormat inFmt  = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date d = inFmt.parse(iso);

            // Output format: 03 Dec 2025
            SimpleDateFormat outFmt = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            return outFmt.format(d);
        } catch (Exception e) {
            return iso;
        }
    }
    private String fileDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MMM-yyyy", Locale.ENGLISH);
        return sdf.format(new Date());
    }



    // =====================================================
    // EXPORT → CSV
    // =====================================================
    private void exportToStorage() {
        StringBuilder sb = new StringBuilder();

// ADD HEADER
        sb.append(BackupFormat.DL_HEADER).append("\n");

        // v3.2: fields quoted so commas in items can't shift columns; importable
        List<Expense> expenses = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        for (Expense e : expenses) {
            sb.append(BackupFormat.row(
                    e.category,
                    e.description == null ? "" : e.description,
                    BackupFormat.plainAmount(e.amount),
                    fmt(e.date)));
        }
        csvContent = sb.toString();


        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE,
                "Expenses_" + fileDate() + ".csv");

        startActivityForResult(intent, CREATE_CSV_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (SummaryExport.onActivityResult(this, requestCode, resultCode, data)) return;
        if (DataTransfer.onActivityResult(this, requestCode, resultCode, data)) return;

        if (requestCode == CREATE_CSV_FILE && resultCode == RESULT_OK) {
            if (data != null && data.getData() != null && csvContent != null) {
                try (OutputStream os = getContentResolver().openOutputStream(data.getData())) {
                    if (os != null) {
                        os.write(csvContent.getBytes());
                        Toast.makeText(this, "CSV exported successfully", Toast.LENGTH_SHORT).show();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "CSV export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    // =====================================================
    // EXPORT → 12-MONTH CATEGORY SUMMARY (v3.1)
    // =====================================================
    private void chooseSummaryPeriod() {
        ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
        List<Expense> expenses = db.expenseDao().getAll();
        List<AccountEntity> accounts = db.accountDao().getAllAccounts();
        List<AccountItemEntity> items = CategorySummary.withoutOrphans(
                db.accountItemDao().getAllItems(), accounts);

        String nowYm = CategorySummary.currentYm();
        List<String> ends = CategorySummary.anchoredPeriodEnds(expenses, items, nowYm);

        String[] labels = new String[ends.size()];
        for (int i = 0; i < ends.size(); i++) {
            CategorySummary s = CategorySummary.build(null, null, null, ends.get(i));
            labels[i] = s.displayLabel(nowYm) + (s.isInProgress(nowYm) ? "  (in progress)" : "");
        }

        new AlertDialog.Builder(this)
                .setTitle("Choose 12-month period")
                .setItems(labels, (d, which) -> {
                    CategorySummary summary =
                            CategorySummary.build(expenses, items, accounts, ends.get(which),
                                    AppPrefs.getCurrencyCode(this));
                    SummaryExport.show(this, summary);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // =====================================================
    // EXPORT → EMAIL
    // =====================================================
    private void exportAndEmail() {
        try {
            File file = new File(
                    getExternalFilesDir(null),
                    "Expenses_" + fileDate() + ".html"
            );

            FileWriter writer = new FileWriter(file);

            writer.append("<html><body><h2>Expenses Export</h2><table border='1'>");
            writer.append("<tr><th>Category</th><th>Item</th><th>Amount</th><th>Date</th></tr>");
            List<Expense> expenses = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
            for (Expense e : expenses) {
                writer.append("<tr>")
                        .append("<td>").append(e.category).append("</td>")
                        .append("<td>").append(e.description).append("</td>")
                        .append("<td>").append(String.valueOf(e.amount)).append("</td>")
                        .append("<td>").append(fmt(e.date)).append("</td>")

                        .append("</tr>");
            }
            writer.append("</table></body></html>");
            writer.flush();
            writer.close();

            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );

            Intent emailIntent = new Intent(Intent.ACTION_SEND);
            emailIntent.setType("text/html");
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Expense Export");
            emailIntent.putExtra(Intent.EXTRA_STREAM, uri);
            emailIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(emailIntent, "Send email..."));

        } catch (Exception e) {
            Toast.makeText(this, "Export failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
