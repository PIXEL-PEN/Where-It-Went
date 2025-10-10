package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.view.ViewGroup;


import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryWiseActivity extends AppCompatActivity {

    private LinearLayout expensesContainer;
    private LayoutInflater inflater;
    private String symbol;

    // Display & conversion formats
    private final SimpleDateFormat FRIENDLY = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH); // e.g., "07 Oct. 2025"
    private final SimpleDateFormat ISO      = new SimpleDateFormat("yyyy-MM-dd",  Locale.ENGLISH); // e.g., "2025-10-07"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_wise);

        expensesContainer = findViewById(R.id.categorywise_container);
        inflater = LayoutInflater.from(this);

// === new top-bar spinner setup ===
        Spinner spinnerNav = findViewById(R.id.spinner_nav);
        List<String> navItems = new ArrayList<>();
        Collections.addAll(navItems,
                "Settings",
                "Category Filter",
                "Distribution View",
                "About");

// Adapter: hide selected text so spinner looks like an icon-only button
        ArrayAdapter<String> navAdapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                navItems
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View v = super.getView(position, convertView, parent);
                if (v instanceof TextView) {
                    ((TextView) v).setText(""); // keep the bar icon-only
                }
                return v;
            }
        };
        navAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerNav.setAdapter(navAdapter);

// icon background; keep a valid selection (text is hidden by getView override)
        spinnerNav.setBackgroundResource(R.drawable.spinner_hamburger);
        spinnerNav.setPrompt("");

// Ensure a neutral non-action item exists at index 0 so "Settings" (index 1) will always trigger
        if (navItems.isEmpty() || !"Menu ▾".equals(navItems.get(0))) {
            navItems.add(0, "Menu ▾");
            navAdapter.notifyDataSetChanged();
        }

// Default to neutral item; adapter getView hides its text so the bar stays icon-only
        spinnerNav.setSelection(0, false);

// Handle only real user selections (avoid initial/resume auto-calls)
        final boolean[] userTapped = {false};
        spinnerNav.setOnTouchListener((v, e) -> { userTapped[0] = true; return false; });

        spinnerNav.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                // Always scrub any residual text from the selected view so no "menu/Menu ▾" shows
                spinnerNav.post(() -> {
                    View sv = spinnerNav.getSelectedView();
                    if (sv instanceof android.widget.TextView) {
                        android.widget.TextView tv = (android.widget.TextView) sv;
                        tv.setText("");
                        tv.setAlpha(0f);
                        tv.setMinWidth(0);
                    }
                });

                // Ignore framework auto-selects; act only on real taps
                if (!userTapped[0]) return;
                userTapped[0] = false;

                if (position == 0) {
                    // Neutral — do nothing
                } else {
                    // Safer than hard-coding indices: switch by label
                    String item = String.valueOf(parent.getItemAtPosition(position));
                    if ("Category Filter".equals(item)) {
                        showSimpleFilterDialog();

                    } else if ("Distribution View".equals(item)) {
                        startActivity(new android.content.Intent(CategoryWiseActivity.this, DistributionViewActivity.class));
                    } else if ("Settings".equals(item)) {
                        startActivity(new android.content.Intent(CategoryWiseActivity.this, SettingsActivity.class));
                    } else if ("About".equals(item)) {
                        startActivity(new android.content.Intent(CategoryWiseActivity.this, AboutActivity.class));
                    }
                }

                // Reset to neutral icon-only after handling
                spinnerNav.setSelection(0, false);
                spinnerNav.post(() -> {
                    View sv2 = spinnerNav.getSelectedView();
                    if (sv2 instanceof android.widget.TextView) {
                        android.widget.TextView tv2 = (android.widget.TextView) sv2;
                        tv2.setText("");
                        tv2.setAlpha(0f);
                        tv2.setMinWidth(0);
                    }
                });
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        symbol = CurrencyUtils.symbolFor(code);

        List<Expense> allExpenses = ExpenseDatabase
                .getDatabase(this)
                .expenseDao()
                .getAll();

        rebuildExpenseView(allExpenses);
    }

    private void rebuildExpenseView(List<Expense> expenseList) {
        expensesContainer.removeAllViews();

        if (expenseList == null || expenseList.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("No expenses found.");
            empty.setPadding(dp(16), dp(16), dp(16), dp(16));
            expensesContainer.addView(empty);
            return;
        }

        // Group by category
        Map<String, List<Expense>> grouped = new LinkedHashMap<>();
        for (Expense e : expenseList) {
            grouped.computeIfAbsent(e.category, k -> new ArrayList<>()).add(e);
        }

        // Sort categories by latest expense date (newest first)
        List<Map.Entry<String, List<Expense>>> categories = new ArrayList<>(grouped.entrySet());
        Collections.sort(categories, (a, b) -> latestDate(b.getValue()).compareTo(latestDate(a.getValue())));

        for (Map.Entry<String, List<Expense>> entry : categories) {
            String category = entry.getKey();
            List<Expense> items = entry.getValue();
            Collections.sort(items, Comparator.comparing(e -> e.date));

            // Category banner
            TextView banner = new TextView(this);
            banner.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    dp(29)
            ));
            banner.setBackgroundColor(0xFFE1C699);
            banner.setText(category);
            banner.setTextSize(16);
            banner.setTypeface(Typeface.DEFAULT_BOLD);
            banner.setTextColor(0xFF000000);
            banner.setGravity(android.view.Gravity.CENTER_VERTICAL | android.view.Gravity.START);
            banner.setPadding(dp(16), 0, 0, 0);
            expensesContainer.addView(banner);

            double catTotal = 0.0;

            for (Expense e : items) {
                catTotal += e.amount;

                View row = inflater.inflate(R.layout.item_expense_date_row, expensesContainer, false);

                TextView textDescription = row.findViewById(R.id.text_description);
                TextView textCategory   = row.findViewById(R.id.text_category);
                TextView textAmount     = row.findViewById(R.id.text_amount);

                textDescription.setText(e.description);
                textCategory.setText(formatFullDate(e.date));

                String formatted = String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);
                SpannableString display = new SpannableString(formatted);
                int start = formatted.length() - symbol.length();
                display.setSpan(new RelativeSizeSpan(0.85f), start, formatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textAmount.setText(display);

                // Detail dialog
                row.setOnClickListener(v -> {
                    String details = "Category: " + e.category + "\n"
                            + "Date: " + formatFullDate(e.date) + "\n"
                            + "Item: " + e.description + "\n"
                            + "Amount: " + String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);

                    new AlertDialog.Builder(CategoryWiseActivity.this)
                            .setTitle("Expense Details")
                            .setMessage(details)
                            .setNegativeButton("CLOSE", (d, which) -> d.dismiss())
                            .setNeutralButton("DELETE", (d, which) -> {
                                ExpenseDatabase.getDatabase(CategoryWiseActivity.this)
                                        .expenseDao()
                                        .delete(e);
                                recreate();
                            })
                            .setPositiveButton("EDIT", (d, which) -> {
                                Intent intent = new Intent(CategoryWiseActivity.this, AddExpenseActivity.class);
                                intent.putExtra("expense_id", e.id);
                                startActivity(intent);
                            })
                            .show();
                });

                expensesContainer.addView(row);

                // Divider
                View divider = new View(this);
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1));
                divider.setBackgroundColor(0xFF888888);
                expensesContainer.addView(divider);
            }

            // TOTAL row
            LinearLayout totalRow = new LinearLayout(this);
            totalRow.setOrientation(LinearLayout.HORIZONTAL);
            totalRow.setPadding(dp(12), dp(12), dp(12), dp(12));

            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            label.setText("TOTAL");
            label.setTextSize(18);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(0xFFB71C1C);

            TextView amountTv = new TextView(this);
            amountTv.setTextSize(18);
            amountTv.setTypeface(Typeface.DEFAULT_BOLD);
            amountTv.setTextColor(0xFFB71C1C);

            DecimalFormat df = new DecimalFormat("#,##0.00");
            String totalFormatted = df.format(catTotal) + " " + symbol;
            SpannableString totalDisplay = new SpannableString(totalFormatted);
            int start = totalFormatted.length() - symbol.length();
            totalDisplay.setSpan(new RelativeSizeSpan(0.85f), start, totalFormatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            amountTv.setText(totalDisplay);

            totalRow.addView(label);
            totalRow.addView(amountTv);
            expensesContainer.addView(totalRow);
        }
    }

    // === Filter dialog with category separator ===
    private void showSimpleFilterDialog() {
        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        // Hardwired defaults in required order
        List<String> defaults = new ArrayList<>();
        Collections.addAll(defaults, "Groceries", "Rent", "Utilities", "Bills", "Transport", "Other");

        // Custom categories appended after defaults
        List<String> customs = new ArrayList<>();
        for (Expense e : all) {
            if (e.category != null && !e.category.trim().isEmpty()
                    && !defaults.contains(e.category) && !customs.contains(e.category)) {
                customs.add(e.category);
            }
        }
        Collections.sort(customs);

        List<String> categories = new ArrayList<>();
        categories.addAll(defaults);
        if (!customs.isEmpty()) {
            categories.add("⋯");
            categories.addAll(customs);
        }

        // Adapter with disabled divider + subtle gray tint
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this,
                android.R.layout.simple_spinner_item,
                categories
        ) {
            @Override
            public boolean isEnabled(int position) {
                String item = getItem(position);
                return item != null && !item.equals("⋯");
            }

            @Override
            public android.view.View getDropDownView(int position, android.view.View convertView, android.view.ViewGroup parent) {
                android.view.View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                String item = getItem(position);
                if ("⋯".equals(item)) {
                    tv.setTextColor(0xFF777777); // consistent divider color
                } else {
                    tv.setTextColor(0xFF000000);
                }
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Build layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(dp(20), dp(12), dp(20), dp(8));

        Spinner spinner = new Spinner(this);
        spinner.setAdapter(adapter);
        layout.addView(spinner);

        TextView txtAvailable = new TextView(this);
        txtAvailable.setTextColor(0xFF666666);
        txtAvailable.setTextSize(14);
        txtAvailable.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
        txtAvailable.setPadding(0, dp(8), 0, dp(12));
        txtAvailable.setText("Available: —");
        layout.addView(txtAvailable);

        TextView txtStart = new TextView(this);
        txtStart.setText("▶ Start Date: (tap to select)");
        txtStart.setTextSize(16);
        txtStart.setTypeface(Typeface.DEFAULT_BOLD);
        txtStart.setPadding(0, dp(4), 0, dp(12));
        layout.addView(txtStart);

        TextView txtEnd = new TextView(this);
        txtEnd.setText("▶ End Date: (tap to select)");
        txtEnd.setTextSize(16);
        txtEnd.setTypeface(Typeface.DEFAULT_BOLD);
        txtEnd.setPadding(0, dp(4), 0, dp(10));
        layout.addView(txtEnd);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = categories.get(position);
                List<Date> dates = new ArrayList<>();
                for (Expense e : all) {
                    if (selected.equals(e.category)) {
                        Date d = tryParseDbText(e.date);
                        if (d != null) dates.add(d);
                    }
                }
                if (!dates.isEmpty()) {
                    String avail = FRIENDLY.format(Collections.min(dates)) + " – " +
                            FRIENDLY.format(Collections.max(dates));
                    txtAvailable.setText("Available: " + avail);
                } else {
                    txtAvailable.setText("Available: (no data)");
                }
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        View.OnClickListener pickDate = v -> {
            boolean isStart = (v == txtStart);
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(CategoryWiseActivity.this, (view, y, m, d) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(y, m, d);
                String label = FRIENDLY.format(chosen.getTime());
                if (isStart) {
                    txtStart.setText("▶ Start Date: " + label);
                } else {
                    txtEnd.setText("▶ End Date: " + label);
                }
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        };
        txtStart.setOnClickListener(pickDate);
        txtEnd.setOnClickListener(pickDate);

        new AlertDialog.Builder(this)
                .setTitle("Filter by Category & Date Range")
                .setView(layout)
                .setPositiveButton("Apply", (d, which) -> {
                    String cat = spinner.getSelectedItem().toString();
                    String startFriendly = txtStart.getText().toString().replace("▶ Start Date: ", "");
                    String endFriendly   = txtEnd.getText().toString().replace("▶ End Date: ", "");

                    if (startFriendly.contains("(") || endFriendly.contains("(")) {
                        String availLine = txtAvailable.getText().toString();
                        if (availLine.startsWith("Available: ") && availLine.contains("–")) {
                            String[] parts = availLine.substring("Available: ".length()).split("–");
                            if (parts.length == 2) {
                                startFriendly = parts[0].trim();
                                endFriendly   = parts[1].trim();
                            }
                        }
                    }

                    String startIso = friendlyToIso(startFriendly);
                    String endIso   = friendlyToIso(endFriendly);

                    List<Expense> filtered = ExpenseDatabase
                            .getDatabase(CategoryWiseActivity.this)
                            .expenseDao()
                            .getExpensesBetweenIso(startIso, endIso);

                    List<Expense> byCat = new ArrayList<>();
                    for (Expense e : filtered) {
                        if (cat.equals(e.category)) byCat.add(e);
                    }

                    rebuildExpenseView(byCat);
                })
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .show();
    }

    // --- Helpers ---

    private Date tryParseDbText(String text) {
        try { return FRIENDLY.parse(text); }
        catch (ParseException e) { return null; }
    }

    private String friendlyToIso(String text) {
        try { return ISO.format(FRIENDLY.parse(text)); }
        catch (Exception e) { return "1970-01-01"; }
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    private String latestDate(List<Expense> list) {
        String latest = "";
        for (Expense e : list) {
            if (e.date != null && e.date.compareTo(latest) > 0) {
                latest = e.date;
            }
        }
        return latest;
    }

    private String formatFullDate(String raw) {
        try {
            SimpleDateFormat in = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date d = in.parse(raw);
            return FRIENDLY.format(d);
        } catch (Exception e) {
            return raw;
        }
    }
}
