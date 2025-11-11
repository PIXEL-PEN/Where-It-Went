package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Build;
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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class CategoryWiseActivity extends AppCompatActivity {

    private LinearLayout categoryContainer;
    private LayoutInflater inflater;

    // Formats
    private final SimpleDateFormat FRIENDLY = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
    private final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    // Accent & headers
    private static final int HEADER_BG_CUSTOM = 0xFFBFCBD3;   // medium gray
    private static final int HEADER_BG_FIXED  = 0xFFFFDDC1;   // salmon

    private static final List<String> FIXED_TOP_ORDER = Arrays.asList(
            "Groceries", "Rent", "Utilities", "Bills", "Transport", "Other"
    );

    // Optional override when a filter is applied
    private List<Expense> overrideFiltered = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_wise);

        inflater = LayoutInflater.from(this);
        categoryContainer = findViewById(R.id.categorywise_container);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        boolean shouldOpen = getIntent().getBooleanExtra("open_filter", false);
        if (shouldOpen) {
            getIntent().removeExtra("open_filter");
            getWindow().getDecorView().postDelayed(() -> {
                if (!isFinishing() && !isDestroyed()) showFilterDialog();
            }, 50);
        }


        // Toolbar filter icon
        ImageButton btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(this::onFilterClick);
        }

        // Drawer link (inside included drawer layout)
        View drawerRoot = findViewById(R.id.include_drawer);
        if (drawerRoot != null) {
            View drawerLink = drawerRoot.findViewById(R.id.linkCategoryFilter);
            if (drawerLink != null) {
                drawerLink.setOnClickListener(this::onFilterClick);
            }
        }
    }





    private void openFilterDialogWithDelay() {
        // 180–250 ms delay allows drawer to finish closing animation
        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                showFilterDialog();  // call the implemented dialog method
            }
        }, 200);
    }

    public void onFilterClick(View v) {
        // Close drawer/scrim first
        View scrim = findViewById(R.id.scrim_overlay);
        View drawer = findViewById(R.id.include_drawer);

        if (scrim != null && scrim.getVisibility() == View.VISIBLE) {
            scrim.setVisibility(View.GONE);
        }
        if (drawer != null && drawer.getVisibility() == View.VISIBLE) {
            drawer.setVisibility(View.GONE);
        }

        // Then open the dialog after a short delay
        openFilterDialogWithDelay();
    }





    @Override
    protected void onResume() {
        super.onResume();
        render();
    }



    private void render() {
        // Load base data (no global DateRangeCutoff — neutralized)
        List<Expense> base = (overrideFiltered != null)
                ? overrideFiltered
                : ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        // Group by category
        Map<String, List<Expense>> byCategory = new LinkedHashMap<>();
        for (Expense e : base) {
            String cat = (e.category == null || e.category.trim().isEmpty()) ? "Uncategorized" : e.category.trim();
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(e);
        }

        // Sort items inside a category: newest date first (then id desc)
        for (List<Expense> items : byCategory.values()) {
            Collections.sort(items, (e1, e2) -> {
                java.util.Date d1 = tryParseAny(e1.date);
                java.util.Date d2 = tryParseAny(e2.date);
                if (d1 != null && d2 != null) {
                    int cmp = d2.compareTo(d1);
                    if (cmp != 0) return cmp;
                } else if (d1 == null ^ d2 == null) {
                    return (d1 == null) ? 1 : -1;
                }
                return Integer.compare(e2.id, e1.id);
            });
        }

        // Order categories: fixed first (in FIXED_TOP_ORDER), then customs A→Z
        List<String> allCats = new ArrayList<>(byCategory.keySet());
        List<String> ordered = new ArrayList<>();
        for (String fixed : FIXED_TOP_ORDER) {
            String key = findKeyIgnoreCase(allCats, fixed);
            if (key != null) ordered.add(key);
        }
        List<String> customs = new ArrayList<>();
        for (String c : allCats) {
            if (!containsIgnoreCase(FIXED_TOP_ORDER, c)) customs.add(c);
        }
        Collections.sort(customs, String::compareToIgnoreCase);
        ordered.addAll(customs);

        // Currency
        String code = getSharedPreferences("settings", MODE_PRIVATE).getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);
        DecimalFormat money = new DecimalFormat("#,##0.00");

        // Build UI
        categoryContainer.removeAllViews();

        final int accentText = ContextCompat.getColor(this, R.color.colorAccent2);

        for (String cat : ordered) {
            List<Expense> items = byCategory.get(cat);
            if (items == null || items.isEmpty()) continue;

            boolean isFixed = containsIgnoreCase(FIXED_TOP_ORDER, cat);

            double catTotal = 0.0;
            for (Expense e : items) catTotal += e.amount;

            // Section (collapsible content)
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sectionLp.setMargins(dp(12), 0, dp(12), dp(8));
            section.setLayoutParams(sectionLp);
            section.setBackgroundColor(0xFFFFFFFF);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) section.setElevation(1f);

            for (int i = 0; i < items.size(); i++) {
                Expense e = items.get(i);
                View row = inflater.inflate(R.layout.item_expense_date_row, section, false);

                TextView textDescription = row.findViewById(R.id.text_description);
                TextView textCategory = row.findViewById(R.id.text_category);
                TextView textAmount = row.findViewById(R.id.text_amount);
                if (textDescription == null || textCategory == null || textAmount == null) continue;

                textDescription.setText(e.description);
                textCategory.setText(safeFriendly(e.date));

                String amt = String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);
                SpannableString amtSpan = new SpannableString(amt);
                int start = amt.length() - symbol.length();
                amtSpan.setSpan(new RelativeSizeSpan(0.85f), start, amt.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textAmount.setText(amtSpan);

                final String catFinal = cat;
                row.setOnClickListener(v -> {
                    String details = "Category: " + catFinal + "\n"
                            + "Date: " + safeFriendly(e.date) + "\n"
                            + "Item: " + e.description + "\n"
                            + "Amount: " + String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);

                    new AlertDialog.Builder(CategoryWiseActivity.this)
                            .setTitle("Expense Details")
                            .setMessage(details)
                            .setNegativeButton("CLOSE", (d, which) -> d.dismiss())
                            .setNeutralButton("DELETE", (d, which) -> {
                                ExpenseDatabase.getDatabase(CategoryWiseActivity.this)
                                        .expenseDao().delete(e);
                                recreate();
                            })
                            .setPositiveButton("EDIT", (d, which) -> {
                                Intent intent = new Intent(CategoryWiseActivity.this, AddExpenseActivity.class);
                                intent.putExtra("expense_id", e.id);
                                startActivity(intent);
                            })
                            .show();
                });

                section.addView(row);

                if (i < items.size() - 1) {
                    View divider = new View(this);
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                    lp.setMargins(dp(12), 0, dp(12), 0);
                    divider.setLayoutParams(lp);
                    divider.setBackgroundColor(0x1A000000);
                    section.addView(divider);
                }
            }

            // TOTAL row
            LinearLayout totalRow = new LinearLayout(this);
            totalRow.setOrientation(LinearLayout.HORIZONTAL);
            totalRow.setPadding(dp(12), dp(10), dp(12), dp(12));

            TextView label = new TextView(this);
            label.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            label.setText("TOTAL");
            label.setTextSize(15);
            label.setTypeface(Typeface.DEFAULT_BOLD);
            label.setTextColor(0xFFB71C1C);

            TextView amountTv = new TextView(this);
            amountTv.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            String totalFormatted = money.format(catTotal) + " " + symbol;
            SpannableString totalDisplay = new SpannableString(totalFormatted);
            int tStart = totalFormatted.length() - symbol.length();
            totalDisplay.setSpan(new RelativeSizeSpan(0.85f), tStart, totalFormatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            amountTv.setText(totalDisplay);
            amountTv.setTextSize(15);
            amountTv.setTypeface(Typeface.DEFAULT_BOLD);
            amountTv.setTextColor(0xFFB71C1C);

            totalRow.addView(label);
            totalRow.addView(amountTv);
            section.addView(totalRow);

            // Header (32dp)
            LinearLayout headerRow = new LinearLayout(this);
            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(32));
            headerLp.setMargins(dp(12), dp(8), dp(12), dp(4));
            headerRow.setLayoutParams(headerLp);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setBackgroundColor(isFixed ? HEADER_BG_FIXED : HEADER_BG_CUSTOM);
            headerRow.setPadding(dp(12), dp(4), dp(12), dp(4));
            headerRow.setGravity(android.view.Gravity.CENTER_VERTICAL);
            headerRow.setClickable(true);

            TextView leftTitle = new TextView(this);
            leftTitle.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
            leftTitle.setText(cat);
            leftTitle.setTextSize(16);
            leftTitle.setTypeface(Typeface.DEFAULT_BOLD);
            leftTitle.setTextColor(accentText);

            TextView rightTotal = new TextView(this);
            rightTotal.setLayoutParams(new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT));
            rightTotal.setText(money.format(catTotal) + " " + symbol);
            rightTotal.setTextSize(14);
            rightTotal.setTypeface(Typeface.DEFAULT_BOLD);
            rightTotal.setTextColor(accentText);

            headerRow.addView(leftTitle);
            headerRow.addView(rightTotal);

            // collapsed by default
            section.setVisibility(View.GONE);
            headerRow.setOnClickListener(v -> {
                section.setVisibility(section.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });

            categoryContainer.addView(headerRow);
            categoryContainer.addView(section);
        }
    }

    // ===== Filter dialog (programmatic, proven working) =====
    private void showFilterDialog() {
        // Build category list: defaults first, "⋯" separator, then customs A→Z
        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        List<String> defaults = new ArrayList<>(FIXED_TOP_ORDER);
        List<String> customs = new ArrayList<>();
        for (Expense e : all) {
            String c = (e.category == null) ? "" : e.category.trim();
            if (!c.isEmpty() && !containsIgnoreCase(defaults, c) && !containsIgnoreCase(customs, c)) {
                customs.add(c);
            }
        }
        Collections.sort(customs, String.CASE_INSENSITIVE_ORDER);

        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        if (!defaults.isEmpty()) categories.addAll(defaults);
        if (!customs.isEmpty()) {
            categories.add("⋯");
            categories.addAll(customs);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                this, android.R.layout.simple_spinner_item, categories) {
            @Override
            public boolean isEnabled(int position) {
                String item = getItem(position);
                return item != null && !item.equals("⋯");
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                TextView tv = (TextView) view;
                String item = getItem(position);
                tv.setTextColor("⋯".equals(item) ? 0xFF777777 : 0xFF000000);
                return view;
            }
        };
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

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

        // Show available range when category changes
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String selected = categories.get(position);
                if ("⋯".equals(selected)) return;
                List<java.util.Date> dates = new ArrayList<>();
                for (Expense e : all) {
                    if ("All Categories".equals(selected) || equalsIgnoreCase(selected, e.category)) {
                        java.util.Date d = tryParseAny(e.date);
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

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
            }
        });

        View.OnClickListener pickDate = v -> {
            boolean isStart = (v == txtStart);
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(CategoryWiseActivity.this, (view, y, m, d) -> {
                Calendar chosen = Calendar.getInstance();
                chosen.set(y, m, d, 0, 0, 0);
                String label = FRIENDLY.format(chosen.getTime());
                if (isStart) txtStart.setText("▶ Start Date: " + label);
                else txtEnd.setText("▶ End Date: " + label);
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        };
        txtStart.setOnClickListener(pickDate);
        txtEnd.setOnClickListener(pickDate);

        new AlertDialog.Builder(this)
                .setTitle("Filter by Category & Date Range")
                .setView(layout)
                .setPositiveButton("Apply", (d, which) -> {
                    String selCat = spinner.getSelectedItem().toString();

                    String startFriendly = txtStart.getText().toString().replace("▶ Start Date: ", "");
                    String endFriendly = txtEnd.getText().toString().replace("▶ End Date: ", "");

                    // If user didn’t pick dates, fall back to available range if present
                    if (startFriendly.contains("(") || endFriendly.contains("(")) {
                        String availLine = txtAvailable.getText().toString();
                        if (availLine.startsWith("Available: ") && availLine.contains("–")) {
                            String[] parts = availLine.substring("Available: ".length()).split("–");
                            if (parts.length == 2) {
                                startFriendly = parts[0].trim();
                                endFriendly = parts[1].trim();
                            }
                        }
                    }

                    String startIso = friendlyToIso(startFriendly);
                    String endIso = friendlyToIso(endFriendly);

                    // Filter DB by ISO range (inclusive) then by category (if not "All Categories")
                    List<Expense> ranged = ExpenseDatabase.getDatabase(CategoryWiseActivity.this)
                            .expenseDao().getExpensesBetweenIso(startIso, endIso);

                    List<Expense> byCat = new ArrayList<>();
                    if ("All Categories".equals(selCat)) {
                        byCat = ranged;
                    } else {
                        for (Expense e : ranged) {
                            if (equalsIgnoreCase(selCat, e.category)) byCat.add(e);
                        }
                    }

                    // Render with override (no global cutoff)
                    overrideFiltered = byCat;
                    render();
                })
                .setNegativeButton("Cancel", (d, which) -> d.dismiss())
                .show();
    }

    // ===== Helpers =====
    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    private java.util.Date tryParseAny(String raw) {
        if (raw == null) return null;
        String[] patterns = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "dd MMM yyyy",
                "dd MMM. yyyy",
                "d MMM yyyy",
                "d MMM. yyyy",
                "d MMMM yyyy"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                return f.parse(raw);
            } catch (Exception ignore) {
            }
        }
        try {
            return FRIENDLY.parse(raw);
        } catch (ParseException e) {
            return null;
        }
    }

    private String safeFriendly(String raw) {
        java.util.Date d = tryParseAny(raw);
        if (d != null) return FRIENDLY.format(d);
        return raw;
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static boolean containsIgnoreCase(Iterable<String> list, String probe) {
        if (probe == null) return false;
        for (String s : list) if (s != null && s.equalsIgnoreCase(probe)) return true;
        return false;
    }

    private static String findKeyIgnoreCase(Iterable<String> iterable, String probe) {
        if (probe == null) return null;
        for (String s : iterable) if (s != null && s.equalsIgnoreCase(probe)) return s;
        return null;
    }

    private String friendlyToIso(String text) {
        try {
            return ISO.format(FRIENDLY.parse(text));
        } catch (Exception e) {
            return "1970-01-01";
        }
    }

}
