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

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // About text
        TextView about = findViewById(R.id.text_about);
        if (about != null) {
            about.setText(Html.fromHtml(getString(R.string.about_text), Html.FROM_HTML_MODE_LEGACY));
            about.setMovementMethod(LinkMovementMethod.getInstance());
        }

        // -------------------------------
        // CURRENCY SPINNER (Symbol mode)
        // -------------------------------
        spinnerCurrency = findViewById(R.id.spinner_currency);

        final List<String> currencies = Arrays.asList(
                "USD — US Dollar ($)",
                "EUR — Euro (€)",
                "GBP — British Pound (£)",
                "THB — Thai Baht (฿)",
                "JPY — Japanese Yen (¥)",
                "CNY — Chinese Yuan (¥)",
                "INR — Indian Rupee (₹)",
                "AUD — Australian Dollar ($)",
                "CAD — Canadian Dollar ($)",
                "SGD — Singapore Dollar ($)",
                "HKD — Hong Kong Dollar ($)",
                "MYR — Malaysian Ringgit (RM)",
                "VND — Vietnamese Dong (₫)"
        );

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_selected,
                currencies
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCurrency.setAdapter(adapter);

        // Load saved symbol (or default)
        String savedSymbol = prefs.getString("currency_symbol", "$");

        // Restore spinner selection based on symbol
        int restoredIndex = 0;
        for (int i = 0; i < currencies.size(); i++) {
            String entry = currencies.get(i);
            if (entry.contains("(" + savedSymbol + ")")) {
                restoredIndex = i;
                break;
            }
        }
        spinnerCurrency.setSelection(restoredIndex, false);

        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String entry = currencies.get(position);

                // Extract symbol from final "(...)"
                String symbol = entry.substring(entry.lastIndexOf('(') + 1, entry.lastIndexOf(')'));

                prefs.edit().putString("currency_symbol", symbol).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });


        // ----------------------------------------------------
        // EXPORT BUTTON
        // ----------------------------------------------------
        findViewById(R.id.btn_export).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Export Data")
                    .setMessage("Choose export method:")
                    .setPositiveButton("Storage (CSV)", (d, w) -> exportToStorage())
                    .setNegativeButton("Email (HTML)", (d, w) -> exportAndEmail())
                    .setNeutralButton("Cancel", null)
                    .show();
        });

        // ----------------------------------------------------
        // RESET BUTTON
        // ----------------------------------------------------
        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Reset Database")
                    .setMessage("This will delete all expenses. Are you sure?")
                    .setPositiveButton("Yes", (d, w) -> {
                        ExpenseDatabase.getDatabase(this).expenseDao().clearAll();
                        Toast.makeText(this, "All expenses cleared", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
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
        List<Expense> expenses = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        for (Expense e : expenses) {
            sb.append(e.category).append(",")
                    .append(e.description).append(",")
                    .append(e.amount).append(",")
                    .append(fmt(e.date)).append("\n");

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
            writer.append("<tr><th>Category</th><th>Description</th><th>Amount</th><th>Date</th></tr>");
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
