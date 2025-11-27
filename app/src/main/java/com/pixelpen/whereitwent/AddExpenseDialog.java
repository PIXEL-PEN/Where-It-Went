package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.*;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.*;

public class AddExpenseDialog extends DialogFragment {

    private Spinner spinnerCategory;
    private EditText inputItem, inputAmount;
    private TextView textDate, textTag;
    private Button btnSave;

    private String currentCategoryTag;
    private String lastSelectedCategory = "Groceries";
    private boolean suppressSpinnerCallback = false;

    private ArrayAdapter<String> categoryAdapter;
    private final List<String> categories = new ArrayList<>();

    private Integer editingId = null;
    private Expense editingExpense = null;

    // --------------------------------------------------------------------
    // CREATE INSTANCE FOR EDITING
    // --------------------------------------------------------------------
    public static AddExpenseDialog newInstance(int expenseId) {
        AddExpenseDialog d = new AddExpenseDialog();
        Bundle b = new Bundle();
        b.putInt("expense_id", expenseId);
        d.setArguments(b);
        return d;
    }

    // --------------------------------------------------------------------
    // CREATE DIALOG
    // --------------------------------------------------------------------
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_expense, null);

        spinnerCategory = v.findViewById(R.id.spinner_category);
        inputItem = v.findViewById(R.id.input_item);
        inputAmount = v.findViewById(R.id.input_amount);
        textDate = v.findViewById(R.id.text_date);
        textTag = v.findViewById(R.id.text_tag);
        btnSave = v.findViewById(R.id.btn_ok);

        if (getArguments() != null) {
            editingId = getArguments().getInt("expense_id", -1);
        }

        if (editingId != null && editingId > 0) {
            loadForEdit(editingId);
        } else {
            setupSpinner();
            setupDatePicker();
        }

        setupSaveButton();

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setCancelable(true)
                .create();
    }

    // --------------------------------------------------------------------
    // LOAD EXPENSE FOR EDITING
    // --------------------------------------------------------------------
    private void loadForEdit(int id) {
        ExpenseDao dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao();
        editingExpense = dao.getById(id);

        setupSpinner();

        inputItem.setText(editingExpense.description);
        inputAmount.setText(String.valueOf(editingExpense.amount));
        textDate.setText(toUi(editingExpense.date));

        int idx = categories.indexOf(editingExpense.category);
        if (idx >= 0) {
            suppressSpinnerCallback = true;
            spinnerCategory.setSelection(idx);
            suppressSpinnerCallback = false;
            updateTagLineFor(editingExpense.category);
        }

        setupDatePicker();
    }

    // --------------------------------------------------------------------
    // SPINNER
    // --------------------------------------------------------------------
    private void setupSpinner() {

        loadCategoriesIntoSpinner("Groceries");
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (suppressSpinnerCallback) return;

                String selected = categories.get(position);

                // ⭐ MANAGE CATEGORIES
                if (selected.equals("➕ Manage Categories")) {

                    // restore previous selection so spinner doesn't stay on "Manage"
                    int prevIndex = categories.indexOf(lastSelectedCategory);
                    if (prevIndex < 0) prevIndex = 0;

                    suppressSpinnerCallback = true;
                    spinnerCategory.setSelection(prevIndex);
                    suppressSpinnerCallback = false;

                    showManageCategoriesDialog();
                    return;
                }

                // ⭐ IGNORE "⋯" DIVIDER
                if (selected.equals("⋯")) {

                    int prevIndex = categories.indexOf(lastSelectedCategory);
                    if (prevIndex < 0) prevIndex = 0;

                    suppressSpinnerCallback = true;
                    spinnerCategory.setSelection(prevIndex);
                    suppressSpinnerCallback = false;

                    return;
                }

                // ⭐ NORMAL CATEGORY
                lastSelectedCategory = selected;
                updateTagLineFor(selected);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategoryTag = null;
                textTag.setText("(auto insert)");
            }
        });

    }

    private void loadCategoriesIntoSpinner(String desiredSelection) {

        List<String> ordered = CategoryManager.getOrderedCategories(requireContext());

        List<String> defaults = Arrays.asList(
                "Groceries", "Rent", "Utilities", "Bills", "Transport", "Other"
        );

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

        categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_selected,
                categories
        );
        categoryAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);

        spinnerCategory.setAdapter(categoryAdapter);

        int idx = categories.indexOf(desiredSelection);
        if (idx < 0) idx = categories.indexOf("Groceries");
        if (idx < 0) idx = 0;

        suppressSpinnerCallback = true;
        spinnerCategory.setSelection(idx);
        suppressSpinnerCallback = false;

        String sel = categories.get(idx);
        if (!sel.equals("⋯") && !sel.equals("➕ Manage Categories")) {
            lastSelectedCategory = sel;
            updateTagLineFor(sel);
        }
    }

    private void updateTagLineFor(String category) {
        String tag = CategoryManager.getTagForCategory(requireContext(), category);
        textTag.setText((tag == null || tag.trim().isEmpty())
                ? "(auto insert)"
                : tag);
    }

    // --------------------------------------------------------------------
    // DATE PICKER
    // --------------------------------------------------------------------
    private void setupDatePicker() {

        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

        if (editingExpense == null) {
            textDate.setText(sdf.format(today.getTime()));
        }

        textDate.setOnClickListener(v -> {

            Calendar cal = Calendar.getInstance();

            DatePickerDialog dlg = new DatePickerDialog(requireContext(),
                    (view, yr, mo, day) -> {

                        String formatted = String.format(Locale.ENGLISH,
                                "%02d %s %04d",
                                day, getMonthAbbrev(mo), yr);

                        textDate.setText(formatted);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            dlg.show();
        });
    }

    private String getMonthAbbrev(int i) {
        String[] m = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return m[i];
    }

    // --------------------------------------------------------------------
    // SAVE BUTTON  (handles INSERT + UPDATE)
    // --------------------------------------------------------------------
    private void setupSaveButton() {

        btnSave.setOnClickListener(v -> {

            String category = spinnerCategory.getSelectedItem().toString();
            String item = inputItem.getText().toString();
            String amountStr = inputAmount.getText().toString();
            String uiDate = textDate.getText().toString();

            if (item.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(getContext(), "Please enter item and amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            String iso = toIso(uiDate);
            if (iso == null) {
                Toast.makeText(getContext(), "Invalid date format!", Toast.LENGTH_SHORT).show();
                return;
            }

            ExpenseDao dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao();

            if (editingExpense != null) {
                editingExpense.category = category;
                editingExpense.description = item;
                editingExpense.amount = amount;
                editingExpense.date = iso;
                dao.update(editingExpense);

                if (getActivity() instanceof DayDetailActivity) {
                    ((DayDetailActivity) getActivity()).refreshAfterEdit();
                }

            } else {

                Expense e = new Expense();
                e.category = category;
                e.description = item;
                e.amount = amount;
                e.date = iso;
                dao.insert(e);

                if (getActivity() instanceof MainScreen) {

                    try {
                        String monthKey = iso.substring(0, 7);
                        int idx = MonthBuilder.findMonthIndex(monthKey);
                        MainScreen.expandMonthIndex = idx;
                    } catch (Exception ex) {
                        MainScreen.expandMonthIndex = -1;
                    }

                    ((MainScreen) getActivity()).refreshAfterAdd();
                }
            }

            dismiss();
        });
    }


    // --------------------------------------------------------------------
    // DATE HELPERS
    // --------------------------------------------------------------------
    private String toIso(String uiDate) {
        try {
            SimpleDateFormat ui = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            Date d = ui.parse(uiDate);
            SimpleDateFormat iso = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            return iso.format(d);
        } catch (Exception e) {
            return null;
        }
    }

    private String toUi(String iso) {
        try {
            SimpleDateFormat i = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
            Date d = i.parse(iso);
            SimpleDateFormat ui = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
            return ui.format(d);
        } catch (Exception e) {
            return iso;
        }
    }


            // --------------------------------------------------------------------
            // MANAGE CATEGORIES (FULL WORKING IMPLEMENTATION)
            // --------------------------------------------------------------------
            private AlertDialog showManageCategoriesDialog () {

                String[] options = {"Add Category", "Edit Tag", "Delete Category", "Reset Defaults"};

                return new AlertDialog.Builder(requireContext())
                        .setTitle("Manage Categories")
                        .setItems(options, (d, which) -> {

                            switch (which) {

                                case 0:
                                    showAddCategoryDialog();
                                    break;

                                case 1:
                                    showEditCategoryDialog();
                                    break;

                                case 2:
                                    showDeleteCategoryDialog();
                                    break;

                                case 3:
                                    CategoryManager.resetToDefault(requireContext());
                                    setupSpinner();
                                    Toast.makeText(requireContext(), "Defaults restored",
                                            Toast.LENGTH_SHORT).show();
                                    break;
                            }
                        })
                        .show();
            }

            private void showAddCategoryDialog () {

                View dv = requireActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_add_category_tagged, null);

                EditText input = dv.findViewById(R.id.edit_category_name);
                RadioGroup rg = dv.findViewById(R.id.radio_tag_group);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Add Category")
                        .setView(dv)
                        .setPositiveButton("Add", (x, y) -> {
                            String name = input.getText().toString().trim();
                            if (name.isEmpty()) return;

                            int sel = rg.getCheckedRadioButtonId();
                            if (sel == -1) return;

                            RadioButton rb = dv.findViewById(sel);
                            String tag = rb.getText().toString();

                            List<String> cur = CategoryManager.getOrderedCategories(requireContext());
                            if (cur.contains(name)) return;

                            // Save category + tag
                            CategoryManager.saveCategoryWithTag(requireContext(), name, tag);

                            // Ensure spinner reopens at the new category
                            lastSelectedCategory = name;

                            // Reload and select it
                            suppressSpinnerCallback = true;
                            loadCategoriesIntoSpinner(name);
                            suppressSpinnerCallback = false;

                            Toast.makeText(requireContext(),
                                    "Added \"" + name + "\"", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }


            private void showEditCategoryDialog () {

                List<String> items = new ArrayList<>();
                for (String c : CategoryManager.getOrderedCategories(requireContext())) {
                    if (!CategoryManager.isDefaultCategory(c))
                        items.add(c);
                }

                if (items.isEmpty()) {
                    Toast.makeText(requireContext(), "No editable categories",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Collections.sort(items, String.CASE_INSENSITIVE_ORDER);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Edit Tag")
                        .setItems(items.toArray(new String[0]), (d, w) ->
                                showTagChangeDialog(items.get(w)))
                        .show();
            }

            private void showTagChangeDialog (String cat){

                View dv = requireActivity().getLayoutInflater()
                        .inflate(R.layout.dialog_add_category_tagged, null);

                RadioGroup rg = dv.findViewById(R.id.radio_tag_group);
                EditText name = dv.findViewById(R.id.edit_category_name);

                name.setText(cat);
                name.setEnabled(false);

                String curTag = CategoryManager.getTagForCategory(requireContext(), cat);
                if ("Fixed".equals(curTag)) rg.check(R.id.radio_fixed);
                else if ("Necessity".equals(curTag)) rg.check(R.id.radio_basic);
                else rg.check(R.id.radio_discretionary);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Change Tag")
                        .setView(dv)
                        .setPositiveButton("Save", (dialog, w) -> {

                            int sel = rg.getCheckedRadioButtonId();
                            RadioButton rb = dv.findViewById(sel);
                            String newTag = rb.getText().toString();

                            CategoryManager.saveCategoryWithTag(requireContext(), cat, newTag);

                            lastSelectedCategory = cat;
                            setupSpinner();

                            Toast.makeText(requireContext(),
                                    "Updated tag", Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
            }

            private void showDeleteCategoryDialog () {

                List<String> deletable = new ArrayList<>();
                for (String c : CategoryManager.getOrderedCategories(requireContext())) {
                    if (!CategoryManager.isDefaultCategory(c))
                        deletable.add(c);
                }

                if (deletable.isEmpty()) {
                    Toast.makeText(requireContext(), "Nothing to delete",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                Collections.sort(deletable, String.CASE_INSENSITIVE_ORDER);

                new AlertDialog.Builder(requireContext())
                        .setTitle("Delete Category")
                        .setItems(deletable.toArray(new String[0]), (d, w) -> {

                            String toRemove = deletable.get(w);

                            new AlertDialog.Builder(requireContext())
                                    .setTitle("Confirm Delete")
                                    .setMessage("Delete \"" + toRemove + "\" permanently?")
                                    .setPositiveButton("Delete", (d2, w2) -> {

                                        CategoryManager.removeCategory(requireContext(), toRemove);
                                        setupSpinner();

                                        Toast.makeText(requireContext(),
                                                "Deleted \"" + toRemove + "\"",
                                                Toast.LENGTH_SHORT).show();
                                    })
                                    .setNegativeButton("Cancel", null)
                                    .show();
                        })
                        .show();


    }
}
