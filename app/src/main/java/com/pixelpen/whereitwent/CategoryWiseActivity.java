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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import android.content.DialogInterface;



public class CategoryWiseActivity extends AppCompatActivity {

    private LinearLayout categoryContainer;
    private LayoutInflater inflater;

    // ***** FORMATTERS (NO DOTS) *****
    private final SimpleDateFormat FRIENDLY = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
    private final SimpleDateFormat ISO = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private static final int HEADER_BG_CUSTOM = 0xFFBFCBD3;
    private static final int HEADER_BG_FIXED = 0xFFFFE0B2;

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

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainScreen.class));
                finish();
            });
        }

        ImageButton btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) btnFilter.setOnClickListener(this::onFilterClick);

        View drawerRoot = findViewById(R.id.include_drawer);
        if (drawerRoot != null) {
            View drawerLink = drawerRoot.findViewById(R.id.linkCategoryFilter);
            if (drawerLink != null) drawerLink.setOnClickListener(this::onFilterClick);
        }
    }

    private void openFilterDialogWithDelay() {
        getWindow().getDecorView().postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) showFilterDialog();
        }, 180);
    }

    public void onFilterClick(View v) {
        View scrim = findViewById(R.id.scrim_overlay);
        View drawer = findViewById(R.id.include_drawer);

        if (scrim != null) scrim.setVisibility(View.GONE);
        if (drawer != null) drawer.setVisibility(View.GONE);

        openFilterDialogWithDelay();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    // ===========================
    // RENDER CATEGORY LIST
    // ===========================
    private void render() {
        List<Expense> base = (overrideFiltered != null)
                ? overrideFiltered
                : ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        Map<String, List<Expense>> byCategory = new LinkedHashMap<>();
        for (Expense e : base) {
            String cat = (e.category == null || e.category.trim().isEmpty())
                    ? "Uncategorized"
                    : e.category.trim();
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(e);
        }

        // Sort per category
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

        // Order categories
        List<String> allCats = new ArrayList<>(byCategory.keySet());
        List<String> ordered = new ArrayList<>();

        for (String fixed : FIXED_TOP_ORDER) {
            for (String real : allCats)
                if (real.equalsIgnoreCase(fixed)) ordered.add(real);
        }

        for (String c : allCats) {
            boolean fixed = false;
            for (String f : FIXED_TOP_ORDER)
                if (c.equalsIgnoreCase(f)) fixed = true;
            if (!fixed) ordered.add(c);
        }

        categoryContainer.removeAllViews();

        String code = getSharedPreferences("settings", MODE_PRIVATE)
                .getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);
        DecimalFormat money = new DecimalFormat("#,##0.00");

        int accentText = ContextCompat.getColor(this, R.color.colorAccent2);

        for (String cat : ordered) {
            List<Expense> items = byCategory.get(cat);
            if (items == null || items.isEmpty()) continue;

            boolean isFixed = false;
            for (String f : FIXED_TOP_ORDER)
                if (cat.equalsIgnoreCase(f)) isFixed = true;

            double catTotal = 0;
            for (Expense e : items) catTotal += e.amount;

            // --- Build rows ---
            LinearLayout section = new LinearLayout(this);
            section.setOrientation(LinearLayout.VERTICAL);
            LinearLayout.LayoutParams sectionLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            sectionLp.setMargins(dp(12), 0, dp(12), dp(8));
            section.setLayoutParams(sectionLp);
            section.setBackgroundColor(0xFFFFFFFF);
            if (Build.VERSION.SDK_INT >= 21) section.setElevation(1f);

            for (int i = 0; i < items.size(); i++) {
                Expense e = items.get(i);
                View row = inflater.inflate(R.layout.item_expense_date_row, section, false);

                TextView tDesc = row.findViewById(R.id.text_description);
                TextView tCat = row.findViewById(R.id.text_category);
                TextView tAmt = row.findViewById(R.id.text_amount);

                tDesc.setText(e.description);
                tCat.setText(safeFriendly(e.date));

                String amt = money.format(e.amount) + " " + symbol;
                SpannableString span = new SpannableString(amt);
                span.setSpan(new RelativeSizeSpan(0.85f),
                        amt.length() - symbol.length(),
                        amt.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                tAmt.setText(span);

                row.setOnClickListener(v -> showDetails(e, cat, symbol));
                section.addView(row);

                if (i < items.size() - 1) {
                    View div = new View(this);
                    LinearLayout.LayoutParams lp =
                            new LinearLayout.LayoutParams(
                                    LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
                    lp.setMargins(dp(12), 0, dp(12), 0);
                    div.setLayoutParams(lp);
                    div.setBackgroundColor(0x1A000000);
                    section.addView(div);
                }
            }

            // --- Total row ---
            LinearLayout totalRow = new LinearLayout(this);
            totalRow.setOrientation(LinearLayout.HORIZONTAL);
            totalRow.setPadding(dp(12), dp(10), dp(12), dp(12));

            TextView lab = new TextView(this);
            lab.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            lab.setText("TOTAL");
            lab.setTextSize(15);
            lab.setTypeface(Typeface.DEFAULT_BOLD);
            lab.setTextColor(0xFFB71C1C);

            TextView amtTv = new TextView(this);
            String totalFormatted = money.format(catTotal) + " " + symbol;
            SpannableString span2 = new SpannableString(totalFormatted);
            span2.setSpan(new RelativeSizeSpan(0.85f),
                    totalFormatted.length() - symbol.length(),
                    totalFormatted.length(),
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            amtTv.setText(span2);
            amtTv.setTextSize(15);
            amtTv.setTypeface(Typeface.DEFAULT_BOLD);
            amtTv.setTextColor(0xFFB71C1C);

            totalRow.addView(lab);
            totalRow.addView(amtTv);
            section.addView(totalRow);

            // --- Header ---
            LinearLayout header = new LinearLayout(this);
            LinearLayout.LayoutParams hlp =
                    new LinearLayout.LayoutParams(-1, dp(32));
            hlp.setMargins(dp(12), dp(8), dp(12), dp(4));
            header.setLayoutParams(hlp);
            header.setPadding(dp(12), dp(4), dp(12), dp(4));
            header.setBackgroundColor(isFixed ? HEADER_BG_FIXED : HEADER_BG_CUSTOM);
            header.setGravity(android.view.Gravity.CENTER_VERTICAL);

            TextView left = new TextView(this);
            left.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1f));
            left.setText(cat);
            left.setTextSize(16);
            left.setTypeface(Typeface.DEFAULT_BOLD);
            left.setTextColor(accentText);

            TextView right = new TextView(this);
            right.setText(money.format(catTotal) + " " + symbol);
            right.setTextSize(14);
            right.setTypeface(Typeface.DEFAULT_BOLD);
            right.setTextColor(accentText);

            header.addView(left);
            header.addView(right);

            section.setVisibility(View.GONE);
            header.setOnClickListener(v -> {
                section.setVisibility(section.getVisibility() == View.VISIBLE
                        ? View.GONE : View.VISIBLE);
            });

            categoryContainer.addView(header);
            categoryContainer.addView(section);
        }
    }
    private void showDetails(Expense e, String cat, String symbol) {

        // Fetch canonical tag for this category
        String tag = CategoryManager.getTagForCategory(this, cat);

        String catLine;
        if (tag != null && !tag.trim().isEmpty()) {
            catLine = "Category: " + cat + " (" + tag + ")";
        } else {
            catLine = "Category: " + cat;
        }

        String msg = catLine +
                "\nDate: " + safeFriendly(e.date) +
                "\nItem: " + e.description +
                "\nAmount: " + String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);

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
                    Intent i = new Intent(this, AddExpenseActivity.class);
                    i.putExtra("expense_id", e.id);
                    startActivity(i);
                })
                .show();
    }


    // ==================================
    // FILTER DIALOG (DOTLESS EDITION)
    // ==================================
    private void showFilterDialog() {

        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        // Build category list
        List<String> defaults = new ArrayList<>(FIXED_TOP_ORDER);
        List<String> customs = new ArrayList<>();
        for (Expense e : all) {
            String c = (e.category == null ? "" : e.category.trim());
            if (!c.isEmpty()
                    && !containsIgnoreCase(defaults, c)
                    && !containsIgnoreCase(customs, c))
                customs.add(c);
        }
        Collections.sort(customs, String.CASE_INSENSITIVE_ORDER);

        List<String> categories = new ArrayList<>();
        categories.add("All Categories");
        categories.addAll(defaults);
        if (!customs.isEmpty()) categories.add("⋯");
        categories.addAll(customs);

        // Spinner
        ArrayAdapter<String> adapter =
                new ArrayAdapter<String>(this,
                        android.R.layout.simple_spinner_item,
                        categories) {
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
        txtStart.setPadding(0, dp(4), 0, dp(12));
        layout.addView(txtStart);

        TextView txtEnd = new TextView(this);
        txtEnd.setText("▶ End Date: (tap to select)");
        txtEnd.setTextSize(16);
        txtEnd.setTypeface(Typeface.DEFAULT_BOLD);
        txtEnd.setPadding(0, dp(4), 0, dp(10));
        layout.addView(txtEnd);

        // Available range update
        spinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> p, View v, int pos, long id) {
                String sel = categories.get(pos);
                if ("⋯".equals(sel)) return;

                List<java.util.Date> dates = new ArrayList<>();
                for (Expense e : all) {
                    if ("All Categories".equals(sel) || equalsIgnoreCase(sel, e.category)) {
                        java.util.Date d = tryParseAny(e.date);
                        if (d != null) dates.add(d);
                    }
                }

                if (!dates.isEmpty()) {
                    String label = FRIENDLY.format(Collections.min(dates))
                            + " – "
                            + FRIENDLY.format(Collections.max(dates));
                    txtAvailable.setText("Available: " + label);
                } else {
                    txtAvailable.setText("Available: (no data)");
                }
            }

            @Override public void onNothingSelected(android.widget.AdapterView<?> p) {}
        });

        // Date pickers
        View.OnClickListener pickDate = clickView -> {
            boolean pickingStart = (clickView == txtStart);

            Calendar now = Calendar.getInstance();
            int Y = now.get(Calendar.YEAR);
            int M = now.get(Calendar.MONTH);
            int D = now.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog dlg =
                    new DatePickerDialog(CategoryWiseActivity.this,
                            (picker, y, m, d) -> {
                                Calendar chosen = Calendar.getInstance();
                                chosen.set(y, m, d, 0, 0, 0);

                                String label = FRIENDLY.format(chosen.getTime());

                                if (pickingStart)
                                    txtStart.setText("▶ Start Date: " + label);
                                else
                                    txtEnd.setText("▶ End Date: " + label);
                            }, Y, M, D);

            dlg.show();
        };

        txtStart.setOnClickListener(pickDate);
        txtEnd.setOnClickListener(pickDate);

        // APPLY BUTTON
        DialogInterface.OnClickListener applyListener =
                (dialog, whichButton) -> {
                    String selCat = spinner.getSelectedItem().toString();

                    String startFriendly =
                            txtStart.getText().toString().replace("▶ Start Date: ", "").trim();
                    String endFriendly =
                            txtEnd.getText().toString().replace("▶ End Date: ", "").trim();

                    // If no manual date chosen → fallback to available range
                    if (startFriendly.contains("(") || endFriendly.contains("(")) {
                        String avail = txtAvailable.getText().toString();
                        if (avail.startsWith("Available: ") && avail.contains("–")) {
                            String[] parts = avail.substring("Available: ".length()).split("–");
                            if (parts.length == 2) {
                                startFriendly = parts[0].trim();
                                endFriendly = parts[1].trim();
                            }
                        }
                    }

                    String startIso = friendlyToIso(startFriendly);
                    String endIso = friendlyToIso(endFriendly);

                    // Java-based inclusive range filter
                    List<Expense> ranged = new ArrayList<>();
                    for (Expense e : all) {
                        java.util.Date d = tryParseAny(e.date);
                        if (d != null) {
                            String iso = ISO.format(d);
                            if (iso.compareTo(startIso) >= 0 &&
                                    iso.compareTo(endIso) <= 0) {
                                ranged.add(e);
                            }
                        }
                    }

                    List<Expense> result = new ArrayList<>();
                    if ("All Categories".equals(selCat)) {
                        result = ranged;
                    } else {
                        for (Expense e : ranged)
                            if (equalsIgnoreCase(selCat, e.category)) result.add(e);
                    }

                    overrideFiltered = result;
                    render();
                };

        new AlertDialog.Builder(this)
                .setTitle("Filter by Category & Date Range")
                .setView(layout)
                .setPositiveButton("Apply", applyListener)
                .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
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
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                return f.parse(raw);
            } catch (Exception ignore) {}
        }

        try { return FRIENDLY.parse(raw); }
        catch (Exception e) { return null; }
    }

    private String safeFriendly(String raw) {
        java.util.Date d = tryParseAny(raw);
        if (d != null) return FRIENDLY.format(d);
        return raw;
    }

    private String friendlyToIso(String text) {
        if (text == null || text.trim().isEmpty()) return "1970-01-01";

        try {
            return ISO.format(FRIENDLY.parse(text.trim()));
        } catch (Exception e) {
            return "1970-01-01";
        }
    }

    private static boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private static boolean containsIgnoreCase(List<String> list, String probe) {
        for (String s : list)
            if (s != null && s.equalsIgnoreCase(probe)) return true;
        return false;
    }
}
