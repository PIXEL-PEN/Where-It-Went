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
import android.widget.RadioButton;
import android.widget.RadioGroup;
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

public class SettingsActivity extends AppCompatActivity {

    private Spinner spinnerCurrency;

    private static final int CREATE_CSV_FILE = 2001;

    private String csvContent = null;

    private RadioGroup radioDateRange;
    private RadioButton radio1m, radio2m, radio3m;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        ImageButton btnForward = findViewById(R.id.btn_forward);
        if (btnForward != null) {
            btnForward.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);

        TextView about = findViewById(R.id.text_about);
        if (about != null) {
            about.setText(Html.fromHtml(getString(R.string.about_text), Html.FROM_HTML_MODE_LEGACY));
            about.setMovementMethod(LinkMovementMethod.getInstance());
            about.setLinksClickable(true);
        }

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

        ArrayAdapter<String> currencyAdapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_item_selected,
                currencies
        );
        currencyAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCurrency.setAdapter(currencyAdapter);

        String defaultCode = currencies.get(0).split(" ")[0];
        String savedCode = prefs.getString("currency_code", defaultCode);

        int restoredIndex = 0;
        for (int i = 0; i < currencies.size(); i++) {
            if (currencies.get(i).startsWith(savedCode + " ")) {
                restoredIndex = i;
                break;
            }
        }
        spinnerCurrency.setSelection(restoredIndex, false);

        spinnerCurrency.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String label = currencies.get(position);
                String code = label.split(" ")[0];
                prefs.edit().putString("currency_code", code).apply();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        radioDateRange = findViewById(R.id.radio_date_range);
        radio1m = findViewById(R.id.radio_1m);
        radio2m = findViewById(R.id.radio_2m);
        radio3m = findViewById(R.id.radio_3m);

        if (radioDateRange != null) {
            int months = DateRangePrefs.getMonths(this);
            if (months == 1) {
                radio1m.setChecked(true);
            } else if (months == 2) {
                radio2m.setChecked(true);
            } else {
                radio3m.setChecked(true);
            }

            radioDateRange.setOnCheckedChangeListener((group, checkedId) -> {
                int selected = 3;
                if (checkedId == R.id.radio_1m) selected = 1;
                else if (checkedId == R.id.radio_2m) selected = 2;
                DateRangePrefs.setMonths(this, selected);
                Toast.makeText(this, "Date range: " + selected + (selected == 1 ? " month" : " months"), Toast.LENGTH_SHORT).show();
            });
        }

        findViewById(R.id.btn_export).setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Export Data")
                    .setMessage("Choose export method:")
                    .setPositiveButton("Storage (CSV)", (d, w) -> exportToStorage())
                    .setNegativeButton("Email (HTML)", (d, w) -> exportAndEmail())
                    .setNeutralButton("Cancel", null)
                    .show();
        });

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

    private void exportToStorage() {
        StringBuilder sb = new StringBuilder();
        List<Expense> expenses = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        for (Expense e : expenses) {
            sb.append(e.category).append(",")
                    .append(e.description).append(",")
                    .append(e.amount).append(",")
                    .append(e.date).append("\n");
        }
        csvContent = sb.toString();

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "expenses.csv");
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

    private void exportAndEmail() {
        try {
            File file = new File(getExternalFilesDir(null), "expenses.html");
            FileWriter writer = new FileWriter(file);

            writer.append("<html><body><h2>Expenses Export</h2><table border='1'>");
            writer.append("<tr><th>Category</th><th>Description</th><th>Amount</th><th>Date</th></tr>");
            List<Expense> expenses = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
            for (Expense e : expenses) {
                writer.append("<tr>")
                        .append("<td>").append(e.category).append("</td>")
                        .append("<td>").append(e.description).append("</td>")
                        .append("<td>").append(String.valueOf(e.amount)).append("</td>")
                        .append("<td>").append(e.date).append("</td>")
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
