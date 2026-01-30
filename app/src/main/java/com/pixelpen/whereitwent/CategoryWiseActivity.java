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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.content.DialogInterface;

public class CategoryWiseActivity extends AppCompatActivity {

    private LinearLayout categoryContainer;
    private LayoutInflater inflater;

    private String filterStartFriendly = null;
    private String filterEndFriendly = null;

    private final SimpleDateFormat FRIENDLY =
            new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
    private final SimpleDateFormat ISO =
            new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private static final int HEADER_BG_CUSTOM = 0xFFE6D2A3;
    private static final int HEADER_BG_FIXED = 0xFFBBCCD4;

    private static final List<String> FIXED_TOP_ORDER = Arrays.asList(
            "Groceries", "Rent", "Utilities", "Bills", "Transport", "Other"
    );

    private List<Expense> overrideFiltered = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_wise);

        inflater = LayoutInflater.from(this);
        categoryContainer = findViewById(R.id.categorywise_container);

        ImageButton menu = findViewById(R.id.btn_menu);
        menu.setOnClickListener(v -> showOverflowMenu());

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainScreen.class));
                finish();
            });
        }

        ImageButton btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) btnFilter.setOnClickListener(this::onFilterClick);
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    @Override
    protected void onPause() {
        super.onPause();
        overrideFiltered = null;
    }

    // ===========================
    // RENDER CATEGORY LIST
    // ===========================
    private void render() {

        TextView monthHeader = findViewById(R.id.text_month_header);

        if (overrideFiltered != null && filterStartFriendly != null && filterEndFriendly != null) {
            monthHeader.setText(
                    "Range: " + filterStartFriendly + " – " + filterEndFriendly
            );
        } else {
            Calendar cal = Calendar.getInstance();
            String title =
                    new SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
                            .format(cal.getTime());
            monthHeader.setText(title);

            filterStartFriendly = null;
            filterEndFriendly = null;
        }

        List<Expense> base;
        if (overrideFiltered != null) {
            base = overrideFiltered;
        } else {
            String[] range = currentMonthRange();
            String startIso = range[0];
            String endIso = range[1];

            List<Expense> all =
                    ExpenseDatabase.getDatabase(this)
                            .expenseDao().getAll();

            base = new ArrayList<>();

            for (Expense e : all) {
                String cat = (e.category == null ? "" : e.category.trim());
                String tag = CategoryManager.getTagForCategory(this, cat);

                if ("Off-Budget".equalsIgnoreCase(tag)) {
                    base.add(e);
                    continue;
                }

                java.util.Date d = tryParseAny(e.date);
                if (d != null) {
                    String iso = ISO.format(d);
                    if (iso.compareTo(startIso) >= 0 &&
                            iso.compareTo(endIso) <= 0) {
                        base.add(e);
                    }
                }
            }
        }

        Map<String, List<Expense>> byCategory = new LinkedHashMap<>();
        for (Expense e : base) {
            String cat =
                    (e.category == null || e.category.trim().isEmpty())
                            ? "Uncategorized"
                            : e.category.trim();
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(e);
        }

        for (List<Expense> items : byCategory.values()) {
            Collections.sort(items, (e1, e2) -> {
                java.util.Date d1 = tryParseAny(e1.date);
                java.util.Date d2 = tryParseAny(e2.date);
                if (d1 != null && d2 != null) {
                    int cmp = d2.compareTo(d1);
                    if (cmp != 0) return cmp;
                }
                return e2.id - e1.id;
            });
        }

        categoryContainer.removeAllViews();

        // ✅ SINGLE SOURCE OF TRUTH
        String symbol = AppPrefs.getCurrencySymbol(this);
        DecimalFormat money = new DecimalFormat("#,##0.00");
        int accentText =
                ContextCompat.getColor(this, R.color.colorAccent2);

        for (String cat : byCategory.keySet()) {
            List<Expense> items = byCategory.get(cat);
            if (items == null || items.isEmpty()) continue;

            double catTotal = 0;
            for (Expense e : items) catTotal += e.amount;

            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);

            for (Expense e : items) {
                View row =
                        inflater.inflate(
                                R.layout.item_expense_date_row,
                                section,
                                false
                        );

                TextView tDesc = row.findViewById(R.id.text_description);
                TextView tCat = row.findViewById(R.id.text_category);
                TextView tAmt = row.findViewById(R.id.text_amount);

                tDesc.setText(e.description);
                tCat.setText(safeFriendly(e.date));

                String amt = money.format(e.amount) + " " + symbol;
                SpannableString span = new SpannableString(amt);
                span.setSpan(
                        new RelativeSizeSpan(0.85f),
                        amt.length() - symbol.length(),
                        amt.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
                tAmt.setText(span);

                row.setOnClickListener(v -> showDetails(e, cat, symbol));
                section.addView(row);
            }

            categoryContainer.addView(section);
        }
    }

    private void showDetails(Expense e, String cat, String symbol) {

        String tag = CategoryManager.getTagForCategory(this, cat);

        String catLine =
                (tag != null && !tag.trim().isEmpty())
                        ? "Category: " + cat + " (" + tag + ")"
                        : "Category: " + cat;

        String msg =
                catLine +
                        "\nDate: " + safeFriendly(e.date) +
                        "\nItem: " + e.description +
                        "\nAmount: " +
                        String.format(
                                Locale.ENGLISH,
                                "%.2f %s",
                                e.amount,
                                symbol
                        );

        new AlertDialog.Builder(this)
                .setTitle("Expense Details")
                .setMessage(msg)
                .setNegativeButton("CLOSE", null)
                .setNeutralButton("DELETE", (a, b) -> {
                    ExpenseDatabase.getDatabase(this)
                            .expenseDao().delete(e);
                    recreate();
                })
                .setPositiveButton("EDIT", (a, b) -> {
                    AddExpenseDialog dialog =
                            AddExpenseDialog.newInstance(e.id);
                    dialog.show(
                            getSupportFragmentManager(),
                            "EDIT_EXPENSE"
                    );
                })
                .show();
    }

    // ======================
    // HELPERS
    // ======================
    private int dp(int d) {
        return Math.round(d * getResources().getDisplayMetrics().density);
    }

    private java.util.Date tryParseAny(String raw) {
        if (raw == null) return null;

        String[] patterns = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "dd MMM yyyy",
                "d MMM yyyy",
                "d MMMM yyyy"
        };

        for (String p : patterns) {
            try {
                SimpleDateFormat f =
                        new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                return f.parse(raw);
            } catch (Exception ignore) {
            }
        }

        try {
            return FRIENDLY.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeFriendly(String raw) {
        java.util.Date d = tryParseAny(raw);
        if (d != null) return FRIENDLY.format(d);
        return raw;
    }

    private String[] currentMonthRange() {
        Calendar cal = Calendar.getInstance();

        int Y = cal.get(Calendar.YEAR);
        int M = cal.get(Calendar.MONTH);

        cal.set(Y, M, 1, 0, 0, 0);
        String start = ISO.format(cal.getTime());

        cal.set(
                Y,
                M,
                cal.getActualMaximum(Calendar.DAY_OF_MONTH),
                23,
                59,
                59
        );
        String end = ISO.format(cal.getTime());

        return new String[]{start, end};
    }

    private void showOverflowMenu() {

        String[] items = new String[]{
                "Date Range Filter",
                "Category Manager",
                "Distribution"
        };

        new AlertDialog.Builder(this)
                .setTitle("Actions")
                .setItems(items, (dlg, which) -> {
                    switch (which) {
                        case 0:
                            showFilterDialog();
                            break;

                        case 1:
                            AddExpenseDialog d = new AddExpenseDialog();
                            Bundle args = new Bundle();
                            args.putBoolean("open_manage_categories", true);
                            d.setArguments(args);
                            d.show(getSupportFragmentManager(), "MANAGE_CATEGORIES");
                            break;

                        case 2:
                            startActivity(new Intent(this, DistributionActivity.class));
                            break;
                    }
                })
                .show();
    }

    public void onFilterClick(View v) {

        View scrim = findViewById(R.id.scrim_overlay);
        View drawer = findViewById(R.id.include_drawer);

        if (scrim != null) scrim.setVisibility(View.GONE);
        if (drawer != null) drawer.setVisibility(View.GONE);

        openFilterDialogWithDelay();
    }

    private void openFilterDialogWithDelay() {
        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                showFilterDialog();
            }
        }, 180);
    }

    private void showFilterDialog() {

        new Thread(() -> {

            List<Expense> all =
                    ExpenseDatabase.getDatabase(CategoryWiseActivity.this)
                            .expenseDao()
                            .getAll();

            runOnUiThread(() -> {
                showFilterDialogInternal(all);
            });

        }).start();
    }

    private void showFilterDialogInternal(List<Expense> all) {

        List<String> defaults = new ArrayList<>(FIXED_TOP_ORDER);
        List<String> customs = new ArrayList<>();

        for (Expense e : all) {
            String c = (e.category == null ? "" : e.category.trim());
            if (!c.isEmpty()
                    && !defaults.contains(c)
                    && !customs.contains(c)) {
                customs.add(c);
            }
        }
        Collections.sort(customs, String.CASE_INSENSITIVE_ORDER);

        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        categories.addAll(defaults);
        if (!customs.isEmpty()) categories.add("⋯");
        categories.addAll(customs);

        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_spinner_item,
                        categories
                ) {
                    @Override
                    public boolean isEnabled(int pos) {
                        return !"⋯".equals(getItem(pos));
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
        txtAvailable.setPadding(0, dp(8), 0, dp(12));
        txtAvailable.setTextColor(0xFF666666);
        txtAvailable.setTypeface(Typeface.SANS_SERIF, Typeface.ITALIC);
        txtAvailable.setText("Available: —");
        layout.addView(txtAvailable);

        TextView txtStart = new TextView(this);
        txtStart.setText("▶ Start Date: (tap to select)");
        txtStart.setTextSize(16);
        txtStart.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(txtStart);

        TextView txtEnd = new TextView(this);
        txtEnd.setText("▶ End Date: (tap to select)");
        txtEnd.setTextSize(16);
        txtEnd.setTypeface(Typeface.DEFAULT_BOLD);
        layout.addView(txtEnd);

        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {

                String sel = categories.get(pos);
                if ("⋯".equals(sel)) return;

                List<java.util.Date> dates = new ArrayList<>();
                for (Expense e : all) {
                    if ("All Categories".equals(sel)
                            || sel.equalsIgnoreCase(e.category)) {
                        java.util.Date d = tryParseAny(e.date);
                        if (d != null) dates.add(d);
                    }
                }

                if (!dates.isEmpty()) {
                    txtAvailable.setText(
                            "Available: " +
                                    FRIENDLY.format(Collections.min(dates)) +
                                    " – " +
                                    FRIENDLY.format(Collections.max(dates))
                    );
                } else {
                    txtAvailable.setText("Available: (no data)");
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> p) {
            }
        });

        View.OnClickListener pickDate = v -> {

            boolean isStart = (v == txtStart);
            Calendar now = Calendar.getInstance();

            DatePickerDialog dlg =
                    new DatePickerDialog(
                            CategoryWiseActivity.this,
                            (dp, y, m, d) -> {
                                Calendar c = Calendar.getInstance();
                                c.set(y, m, d);
                                String label = FRIENDLY.format(c.getTime());
                                if (isStart)
                                    txtStart.setText("▶ Start Date: " + label);
                                else
                                    txtEnd.setText("▶ End Date: " + label);
                            },
                            now.get(Calendar.YEAR),
                            now.get(Calendar.MONTH),
                            now.get(Calendar.DAY_OF_MONTH)
                    );
            dlg.show();
        };

        txtStart.setOnClickListener(pickDate);
        txtEnd.setOnClickListener(pickDate);

        new AlertDialog.Builder(this)
                .setTitle("Filter by Category & Date Range")
                .setView(layout)
                .setPositiveButton("Apply", (d, w) -> {

                    String startF =
                            txtStart.getText().toString()
                                    .replace("▶ Start Date:", "")
                                    .trim();
                    String endF =
                            txtEnd.getText().toString()
                                    .replace("▶ End Date:", "")
                                    .trim();

                    String startIso = friendlyToIso(startF);
                    String endIso = friendlyToIso(endF);

                    List<Expense> filtered = new ArrayList<>();
                    for (Expense e : all) {
                        java.util.Date dt = tryParseAny(e.date);
                        if (dt == null) continue;
                        String iso = ISO.format(dt);
                        if (iso.compareTo(startIso) >= 0 &&
                                iso.compareTo(endIso) <= 0) {
                            filtered.add(e);
                        }
                    }

                    filterStartFriendly = startF;
                    filterEndFriendly = endF;
                    overrideFiltered = filtered;
                    render();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String friendlyToIso(String text) {
        if (text == null || text.trim().isEmpty()) {
            return "1970-01-01";
        }

        try {
            return ISO.format(FRIENDLY.parse(text.trim()));
        } catch (Exception e) {
            return "1970-01-01";
        }
    }

}