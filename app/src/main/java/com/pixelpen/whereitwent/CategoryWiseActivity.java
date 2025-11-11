package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
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

    private static final String PREFS = "settings";
    private static final String K_ACTIVE = "catf_active";
    private static final String K_CATEGORY = "catf_category";
    private static final String K_START = "catf_start";
    private static final String K_END = "catf_end";

    private final SimpleDateFormat outDate = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
    private final SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);

    private final String[] parsePatterns = new String[] {
            "yyyy-MM-dd",
            "dd/MM/yyyy",
            "MM/dd/yyyy",
            "dd MMM yyyy",
            "dd MMM. yyyy",
            "d MMM yyyy",
            "d MMM. yyyy",
            "d MMMM yyyy"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_wise);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        ImageButton btnFilter = findViewById(R.id.btn_filter);
        if (btnFilter != null) {
            btnFilter.setOnClickListener(v -> showCategoryFilterDialog());
            btnFilter.setOnLongClickListener(v -> {
                clearCustomFilter();
                return true;
            });
        }

        TextView linkDrawer = findViewById(R.id.linkCategoryFilter);
        if (linkDrawer != null) {
            linkDrawer.setOnClickListener(v -> showCategoryFilterDialog());
        }

        categoryContainer = findViewById(R.id.categorywise_container);
        render();
    }

    @Override
    protected void onResume() {
        super.onResume();
        render();
    }

    private void render() {
        LayoutInflater inflater = LayoutInflater.from(this);
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);

        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);
        DecimalFormat money = new DecimalFormat("#,##0.00");

        List<Expense> base = ExpenseDatabase.getDatabase(this).expenseDao().getAll();

        boolean active = prefs.getBoolean(K_ACTIVE, false);
        String fCat = prefs.getString(K_CATEGORY, "All Categories");
        String fStart = prefs.getString(K_START, null);
        String fEnd = prefs.getString(K_END, null);

        List<Expense> filtered;
        if (active && fStart != null && fEnd != null) {
            filtered = filterByCustomRange(base, fStart, fEnd);
            if (fCat != null && !"All Categories".equalsIgnoreCase(fCat)) {
                filtered = filterByCategory(filtered, fCat);
            }
        } else {
            filtered = DateRangeCutoff.filterByMonths(this, base);
        }

        Map<String, List<Expense>> byCategory = new LinkedHashMap<>();
        for (Expense e : filtered) {
            String cat = (e.category == null || e.category.trim().isEmpty()) ? "Uncategorized" : e.category.trim();
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(e);
        }

        for (List<Expense> items : byCategory.values()) {
            Collections.sort(items, (e1, e2) -> {
                java.util.Date d1 = parseDate(e1.date);
                java.util.Date d2 = parseDate(e2.date);
                if (d1 != null && d2 != null) {
                    int cmp = d2.compareTo(d1);
                    if (cmp != 0) return cmp;
                } else if (d1 == null ^ d2 == null) {
                    return (d1 == null) ? 1 : -1;
                }
                return Integer.compare(e2.id, e1.id);
            });
        }

        List<String> categories = new ArrayList<>(byCategory.keySet());
        Collections.sort(categories, Comparator.nullsLast(String::compareToIgnoreCase));

        categoryContainer.removeAllViews();

        if (active && fStart != null && fEnd != null) {
            addFilterPill(categoryContainer, fCat, fStart, fEnd);
        }

        final int accentText = ContextCompat.getColor(this, R.color.colorAccent2);
        final int headerBg = 0xFFBFCBD3;

        for (String cat : categories) {
            List<Expense> items = byCategory.get(cat);
            if (items == null || items.isEmpty()) continue;

            double catTotal = 0.0;
            for (Expense e : items) catTotal += e.amount;

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
                TextView textCategory   = row.findViewById(R.id.text_category);
                TextView textAmount     = row.findViewById(R.id.text_amount);

                if (textDescription == null || textCategory == null || textAmount == null) continue;

                textDescription.setText(e.description);
                textCategory.setText(safeDateDisplay(e.date));

                String amt = String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);
                SpannableString amtSpan = new SpannableString(amt);
                int start = amt.length() - symbol.length();
                amtSpan.setSpan(new RelativeSizeSpan(0.85f), start, amt.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                textAmount.setText(amtSpan);

                row.setOnClickListener(v -> {
                    String details = "Category: " + cat + "\n"
                            + "Date: " + safeDateDisplay(e.date) + "\n"
                            + "Item: " + e.description + "\n"
                            + "Amount: " + String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);

                    AlertDialog dialog = new AlertDialog.Builder(CategoryWiseActivity.this)
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
                            .create();
                    dialog.show();
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

            LinearLayout headerRow = new LinearLayout(this);
            LinearLayout.LayoutParams headerLp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(40));
            headerLp.setMargins(dp(12), dp(8), dp(12), dp(4));
            headerRow.setLayoutParams(headerLp);
            headerRow.setOrientation(LinearLayout.HORIZONTAL);
            headerRow.setBackgroundColor(headerBg);
            headerRow.setPadding(dp(12), dp(6), dp(12), dp(6));
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

            section.setVisibility(View.GONE);
            headerRow.setOnClickListener(v -> {
                section.setVisibility(section.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            });

            categoryContainer.addView(headerRow);
            categoryContainer.addView(section);
        }
    }

    private void addFilterPill(LinearLayout parent, String cat, String startIso, String endIso) {
        LinearLayout pill = new LinearLayout(this);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(dp(12), dp(8), dp(12), dp(4));
        pill.setLayoutParams(lp);
        pill.setOrientation(LinearLayout.HORIZONTAL);
        pill.setBackgroundColor(0xFFEFF5F9);
        pill.setPadding(dp(10), dp(8), dp(10), dp(8));

        TextView tv = new TextView(this);
        tv.setLayoutParams(new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        String catLabel = (cat == null || cat.trim().isEmpty()) ? "All Categories" : cat;
        String label = "Filtered: " + catLabel + "  •  " + startIso + " – " + endIso;
        tv.setText(label);
        tv.setTextSize(13);
        tv.setTextColor(ContextCompat.getColor(this, R.color.colorAccent2));

        TextView clear = new TextView(this);
        clear.setText("CLEAR");
        clear.setTextSize(13);
        clear.setTypeface(Typeface.DEFAULT_BOLD);
        clear.setTextColor(0xFFB71C1C);
        clear.setPadding(dp(12), 0, 0, 0);
        clear.setOnClickListener(v -> clearCustomFilter());

        pill.addView(tv);
        pill.addView(clear);
        parent.addView(pill);
    }

    private void clearCustomFilter() {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        prefs.edit()
                .putBoolean(K_ACTIVE, false)
                .remove(K_CATEGORY)
                .remove(K_START)
                .remove(K_END)
                .apply();
        render();
    }

    private List<Expense> filterByCustomRange(List<Expense> items, String startIso, String endIso) {
        java.util.Date start = parseIso(startIso);
        java.util.Date end = parseIso(endIso);
        if (start == null || end == null) return new ArrayList<>(items);

        Calendar calEnd = Calendar.getInstance(Locale.ENGLISH);
        calEnd.setTime(end);
        calEnd.set(Calendar.HOUR_OF_DAY, 23);
        calEnd.set(Calendar.MINUTE, 59);
        calEnd.set(Calendar.SECOND, 59);
        calEnd.set(Calendar.MILLISECOND, 999);
        long endMs = calEnd.getTimeInMillis();

        List<Expense> out = new ArrayList<>();
        for (Expense e : items) {
            java.util.Date d = parseDate(e.date);
            if (d == null) continue;
            long ms = d.getTime();
            if (ms >= start.getTime() && ms <= endMs) out.add(e);
        }
        return out;
    }

    private List<Expense> filterByCategory(List<Expense> items, String cat) {
        List<Expense> out = new ArrayList<>();
        for (Expense e : items) {
            String c = (e.category == null) ? "" : e.category.trim();
            if (c.equalsIgnoreCase(cat)) out.add(e);
        }
        return out;
    }

    private void showCategoryFilterDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_filter_by_period, null);

        Spinner spinner = view.findViewById(R.id.spinner_category);
        Button btnStart = view.findViewById(R.id.btn_start_date);
        Button btnEnd = view.findViewById(R.id.btn_end_date);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnApply = view.findViewById(R.id.btn_apply);

        List<Expense> all = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        LinkedHashSet<String> set = new LinkedHashSet<>();
        for (Expense e : all) {
            if (e.category != null && !e.category.trim().isEmpty()) set.add(e.category.trim());
        }
        List<String> cats = new ArrayList<>();
        cats.add("All Categories");
        cats.addAll(set);
        if (cats.size() > 1) {
            Collections.sort(cats.subList(1, cats.size()), String::compareToIgnoreCase);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, cats);
        spinner.setAdapter(adapter);

        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String prevCat = prefs.getString(K_CATEGORY, "All Categories");
        String prevStart = prefs.getString(K_START, null);
        String prevEnd = prefs.getString(K_END, null);

        if (prevCat != null) {
            int idx = cats.indexOf(prevCat);
            if (idx >= 0) spinner.setSelection(idx);
        }
        if (prevStart != null) btnStart.setText(prevStart);
        if (prevEnd != null) btnEnd.setText(prevEnd);

        final Calendar calTmp = Calendar.getInstance();

        btnStart.setOnClickListener(v -> {
            int y = calTmp.get(Calendar.YEAR), m = calTmp.get(Calendar.MONTH), d = calTmp.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(this, (dp, yy, mm, dd) -> {
                calTmp.set(yy, mm, dd, 0, 0, 0);
                btnStart.setText(iso.format(calTmp.getTime()));
            }, y, m, d).show();
        });

        btnEnd.setOnClickListener(v -> {
            int y = calTmp.get(Calendar.YEAR), m = calTmp.get(Calendar.MONTH), d = calTmp.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(this, (dp, yy, mm, dd) -> {
                calTmp.set(yy, mm, dd, 0, 0, 0);
                btnEnd.setText(iso.format(calTmp.getTime()));
            }, y, m, d).show();
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnApply.setOnClickListener(v -> {
            String chooseCat = (String) spinner.getSelectedItem();
            String s = btnStart.getText().toString();
            String e = btnEnd.getText().toString();

            if (s == null || s.length() < 8 || e == null || e.length() < 8) {
                new AlertDialog.Builder(this)
                        .setMessage("Please select both start and end dates.")
                        .setPositiveButton("OK", null)
                        .show();
                return;
            }

            prefs.edit()
                    .putBoolean(K_ACTIVE, true)
                    .putString(K_CATEGORY, chooseCat)
                    .putString(K_START, s)
                    .putString(K_END, e)
                    .apply();

            dialog.dismiss();
            render();
        });

        dialog.show();
    }

    private int dp(int dps) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dps * density);
    }

    private java.util.Date parseDate(String raw) {
        if (raw == null) return null;
        for (String p : parsePatterns) {
            try {
                SimpleDateFormat f = new SimpleDateFormat(p, Locale.ENGLISH);
                f.setLenient(false);
                return f.parse(raw);
            } catch (Exception ignore) {}
        }
        return null;
    }

    private java.util.Date parseIso(String raw) {
        try {
            iso.setLenient(false);
            return iso.parse(raw);
        } catch (Exception e) {
            return null;
        }
    }

    private String safeDateDisplay(String raw) {
        java.util.Date d = parseDate(raw);
        if (d != null) return outDate.format(d);
        return raw;
    }
}
