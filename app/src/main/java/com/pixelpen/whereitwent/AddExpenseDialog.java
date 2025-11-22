package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AddExpenseDialog extends DialogFragment {

    private Spinner spinnerCategory;
    private EditText editDescription, editAmount;
    private TextView textCategoryTag, textDate;

    private ArrayAdapter<String> categoryAdapter;
    private final List<String> categories = new ArrayList<>();
    private String lastSelectedCategory = "Groceries";
    private boolean suppressSpinner = false;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_expense, null);

        spinnerCategory   = v.findViewById(R.id.spinner_category);
        editDescription   = v.findViewById(R.id.edit_description);
        editAmount        = v.findViewById(R.id.edit_amount);
        textCategoryTag   = v.findViewById(R.id.text_category_tag);
        textDate          = v.findViewById(R.id.text_date);

        loadCategories(lastSelectedCategory);
        setupSpinnerListener();
        setupDatePicker();
        setupSaveButton();

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setCancelable(true)
                .create();
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {
            d.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
    }

    // --------------------------------------------------------------------
    // LOAD CATEGORIES
    // --------------------------------------------------------------------
    private void loadCategories(String select) {
        List<String> ordered = CategoryManager.getOrderedCategories(requireContext());

        List<String> defaults = new ArrayList<>();
        Collections.addAll(defaults, "Groceries", "Rent", "Utilities", "Bills", "Transport", "Other");

        List<String> custom = new ArrayList<>();
        for (String c : ordered)
            if (!defaults.contains(c)) custom.add(c);
        Collections.sort(custom, String.CASE_INSENSITIVE_ORDER);

        categories.clear();
        categories.addAll(defaults);

        if (!custom.isEmpty()) {
            categories.add("⋯");
            categories.addAll(custom);
        }

        categories.add("➕ Manage Categories");

        categoryAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_selected,
                categories);

        categoryAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCategory.setAdapter(categoryAdapter);

        int idx = categories.indexOf(select);
        if (idx < 0) idx = categories.indexOf("Groceries");
        if (idx < 0) idx = 0;

        suppressSpinner = true;
        spinnerCategory.setSelection(idx);
        suppressSpinner = false;

        updateTagLine(categories.get(idx));
    }

    // --------------------------------------------------------------------
    // SPINNER LOGIC
    // --------------------------------------------------------------------
    private void setupSpinnerListener() {
        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent,
                                       View view, int pos, long id) {

                if (suppressSpinner) return;

                String sel = categories.get(pos);

                if (sel.equals("⋯")) return;

                if (sel.equals("➕ Manage Categories")) {
                    openCategoryManagement();
                    return;
                }

                lastSelectedCategory = sel;
                updateTagLine(sel);
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private void updateTagLine(String category) {
        String tag = CategoryManager.getTagForCategory(requireContext(), category);
        if (tag == null || tag.isEmpty())
            textCategoryTag.setText("➤ (auto insert)");
        else
            textCategoryTag.setText(" " + tag);
    }

    // --------------------------------------------------------------------
    // MANAGE CATEGORY (OVERLAY)
    // --------------------------------------------------------------------
    private void openCategoryManagement() {

        String[] items = {
                "Add Category",
                "Edit Tag",
                "Delete Category",
                "Reset Defaults"
        };

        new AlertDialog.Builder(requireContext())
                .setTitle("Manage Categories")
                .setItems(items, (d, which) -> {

                    switch (which) {
                        case 0: showAddCategoryDialog(); break;
                        case 1: showEditTagDialog(); break;
                        case 2: showDeleteCategoryDialog(); break;
                        case 3:
                            CategoryManager.resetToDefault(requireContext());
                            Toast.makeText(requireContext(), "Defaults restored", Toast.LENGTH_SHORT).show();
                            reloadSpinner();
                            break;
                    }

                })
                .show();
    }

    private void reloadSpinner() {
        loadCategories(lastSelectedCategory);
    }

    // --------------------------------------------------------------------
    // ADD CATEGORY
    // --------------------------------------------------------------------
    private void showAddCategoryDialog() {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_category_tagged, null);

        EditText input = v.findViewById(R.id.edit_category_name);
        android.widget.RadioGroup group = v.findViewById(R.id.radio_tag_group);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Category")
                .setView(v)
                .setPositiveButton("Add", (d,w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;

                    int id = group.getCheckedRadioButtonId();
                    if (id == -1) return;

                    android.widget.RadioButton rb = v.findViewById(id);

                    CategoryManager.saveCategoryWithTag(requireContext(), name, rb.getText().toString());
                    Toast.makeText(requireContext(), "Added " + name, Toast.LENGTH_SHORT).show();

                    lastSelectedCategory = name;
                    reloadSpinner();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --------------------------------------------------------------------
    // EDIT TAG
    // --------------------------------------------------------------------
    private void showEditTagDialog() {
        List<String> editable = new ArrayList<>();

        for (String c : CategoryManager.getOrderedCategories(requireContext()))
            if (!CategoryManager.isDefaultCategory(c))
                editable.add(c);

        if (editable.isEmpty()) return;

        Collections.sort(editable);
        String[] arr = editable.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Tag")
                .setItems(arr, (d,w) -> showTagSelector(arr[w]))
                .show();
    }

    private void showTagSelector(String categoryName) {
        View v = getLayoutInflater().inflate(R.layout.dialog_add_category_tagged, null);

        android.widget.RadioGroup group = v.findViewById(R.id.radio_tag_group);
        EditText input = v.findViewById(R.id.edit_category_name);

        input.setText(categoryName);
        input.setEnabled(false);

        String tag = CategoryManager.getTagForCategory(requireContext(), categoryName);
        if ("Fixed".equals(tag)) group.check(R.id.radio_fixed);
        else if ("Necessity".equals(tag)) group.check(R.id.radio_basic);
        else group.check(R.id.radio_discretionary);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Tag")
                .setView(v)
                .setPositiveButton("Save", (d,w) -> {
                    int id = group.getCheckedRadioButtonId();
                    android.widget.RadioButton rb = v.findViewById(id);

                    CategoryManager.saveCategoryWithTag(requireContext(),
                            categoryName,
                            rb.getText().toString());

                    Toast.makeText(requireContext(), "Updated", Toast.LENGTH_SHORT).show();
                    reloadSpinner();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // --------------------------------------------------------------------
    // DELETE CATEGORY
    // --------------------------------------------------------------------
    private void showDeleteCategoryDialog() {
        List<String> del = new ArrayList<>();

        for (String c : CategoryManager.getOrderedCategories(requireContext()))
            if (!CategoryManager.isDefaultCategory(c))
                del.add(c);

        if (del.isEmpty()) return;

        Collections.sort(del);
        String[] arr = del.toArray(new String[0]);

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setItems(arr, (d,w) -> {
                    String target = arr[w];

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Delete")
                            .setMessage("Delete \"" + target + "\"?")
                            .setPositiveButton("Delete", (dd,ww) -> {
                                CategoryManager.removeCategory(requireContext(), target);
                                Toast.makeText(requireContext(), "Deleted", Toast.LENGTH_SHORT).show();
                                reloadSpinner();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }

    // --------------------------------------------------------------------
    // DATE PICKER
    // --------------------------------------------------------------------
    private void setupDatePicker() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);

        textDate.setText(sdf.format(now.getTime()));

        textDate.setOnClickListener(v -> {
            DatePickerDialog dp = new DatePickerDialog(requireContext(),
                    (view, y, m, d) ->
                            textDate.setText(String.format(Locale.ENGLISH,
                                    "%02d %s %04d",
                                    d, getMonthAbbrev(m), y)),
                    now.get(Calendar.YEAR),
                    now.get(Calendar.MONTH),
                    now.get(Calendar.DAY_OF_MONTH));

            dp.show();
        });
    }

    private String getMonthAbbrev(int m) {
        final String[] months = {
                "Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.",
                "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."
        };
        return months[m];
    }

    // --------------------------------------------------------------------
    // SAVE
    // --------------------------------------------------------------------
    private void setupSaveButton() {
        View dialogView = getDialog().getWindow().getDecorView();
        View btnSave = dialogView.findViewById(R.id.btn_save);

        btnSave.setOnClickListener(v -> {
            String desc = editDescription.getText().toString().trim();
            String amt  = editAmount.getText().toString().trim();

            if (desc.isEmpty() || amt.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amt);

            Expense e = new Expense();
            e.description = desc;
            e.amount = amount;
            e.date = textDate.getText().toString();
            e.category = spinnerCategory.getSelectedItem().toString();

            ExpenseDatabase.getDatabase(requireContext())
                    .expenseDao()
                    .insert(e);

            Toast.makeText(requireContext(),
                    "Expense added",
                    Toast.LENGTH_SHORT).show();

            dismiss();
        });
    }
}
