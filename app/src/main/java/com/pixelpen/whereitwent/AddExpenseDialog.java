package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
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
    private EditText editDescription, editAmount;
    private TextView textDate, textCategoryTag;
    private Button btnSave;

    // state
    private String currentCategoryTag;
    private String lastSelectedCategory = "Groceries";
    private boolean suppressSpinnerCallback = false;

    private ArrayAdapter<String> categoryAdapter;
    private final List<String> categories = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_expense, null);

        spinnerCategory  = v.findViewById(R.id.spinner_category);
        editDescription  = v.findViewById(R.id.input_item);
        editAmount       = v.findViewById(R.id.input_amount);
        textDate         = v.findViewById(R.id.text_date);
        textCategoryTag  = v.findViewById(R.id.text_tag);
        btnSave          = v.findViewById(R.id.btn_ok);

        setupSpinner();
        setupDatePicker();
        setupSaveButton();

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setCancelable(true)
                .create();
    }

    // --------------------------------------------
    // CATEGORY SPINNER
    // --------------------------------------------
    private void setupSpinner() {

        loadCategoriesIntoSpinner("Groceries");

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (suppressSpinnerCallback) return;

                String selected = categories.get(position);

                if (selected.equals("➕ Manage Categories")) {
                    showManageCategoriesDialog();
                    return;
                }

                if (!selected.equals("⋯")) {
                    currentCategoryTag = selected;
                    lastSelectedCategory = selected;
                    updateTagLineFor(selected);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {
                currentCategoryTag = null;
                textCategoryTag.setText("(auto insert)");
            }
        });
    }

    private void loadCategoriesIntoSpinner(String desiredSelection) {

        List<String> ordered = CategoryManager.getOrderedCategories(requireContext());

        List<String> defaults = Arrays.asList("Groceries","Rent","Utilities","Bills","Transport","Other");

        List<String> custom = new ArrayList<>();
        for (String c : ordered) if (!defaults.contains(c)) custom.add(c);
        Collections.sort(custom, String.CASE_INSENSITIVE_ORDER);

        categories.clear();
        categories.addAll(defaults);

        if (!custom.isEmpty()) {
            categories.add("⋯");
            categories.addAll(custom);
        }

        categories.add("➕ Manage Categories");

        categoryAdapter = new ArrayAdapter<>(requireContext(),
                R.layout.spinner_item_selected, categories);
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
        } else {
            textCategoryTag.setText("(auto insert)");
        }
    }

    private void reloadCategories(String desiredSelection) {
        loadCategoriesIntoSpinner(desiredSelection);
    }

    private void updateTagLineFor(String category) {
        String tag = CategoryManager.getTagForCategory(requireContext(), category);
        textCategoryTag.setText((tag == null || tag.trim().isEmpty())
                ? "(auto insert)"
                : " " + tag);
    }

    private AlertDialog showManageCategoriesDialog() {
        String[] options = {"Add Category", "Edit Tag", "Delete Category", "Reset Defaults"};

        return new AlertDialog.Builder(requireContext())
                .setTitle("Manage Categories")
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: showAddCategoryDialog(); break;
                        case 1: showEditCategoryDialog(); break;
                        case 2: showDeleteCategoryDialog(); break;
                        case 3:
                            CategoryManager.resetToDefault(requireContext());
                            reloadCategories("Groceries");
                            lastSelectedCategory = "Groceries";
                            Toast.makeText(requireContext(), "Defaults restored", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    // --------------------------------------------
    // DATE PICKER
    // --------------------------------------------
    private void setupDatePicker() {

        Calendar today = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
        textDate.setText(sdf.format(today.getTime()));

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
        String[] m = {"Jan.","Feb.","Mar.","Apr.","May","Jun.",
                "Jul.","Aug.","Sep.","Oct.","Nov.","Dec."};
        return m[i];
    }

    // --------------------------------------------
    // SAVE TO DATABASE
    // --------------------------------------------
    private void setupSaveButton() {

        btnSave.setOnClickListener(v -> {

            String desc = editDescription.getText().toString().trim();
            String amtStr = editAmount.getText().toString().trim();
            String date = textDate.getText().toString();
            String category = spinnerCategory.getSelectedItem().toString();

            if (category.equals("➕ Manage Categories") || category.equals("⋯")) {
                Toast.makeText(requireContext(), "Select valid category", Toast.LENGTH_SHORT).show();
                return;
            }
            if (desc.isEmpty() || amtStr.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amtStr);

            String finalCategory = (currentCategoryTag != null && !currentCategoryTag.isEmpty())
                    ? currentCategoryTag
                    : category;

            Expense e = new Expense();
            e.category = finalCategory;
            e.date = date;
            e.description = desc;
            e.amount = amount;

            ExpenseDatabase.getDatabase(requireContext())
                    .expenseDao().insert(e);

            Toast.makeText(requireContext(), "Expense added", Toast.LENGTH_SHORT).show();

            // After insert → open DateWiseActivity
            Intent intent = new Intent(requireContext(), DateWiseActivity.class);
            intent.putExtra("from_add_expense", true);
            requireActivity().startActivity(intent);

            dismiss();
        });
    }

    // --------------------------------------------
    // CATEGORY DIALOGS (unchanged from MainActivity)
    // --------------------------------------------

    private void showAddCategoryDialog() {
        View dialogView = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_add_category_tagged, null);

        EditText input = dialogView.findViewById(R.id.edit_category_name);
        RadioGroup rg = dialogView.findViewById(R.id.radio_tag_group);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Category")
                .setView(dialogView)
                .setPositiveButton("Add", (d,w)->{
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(requireContext(),"Enter name",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int sel = rg.getCheckedRadioButtonId();
                    if (sel == -1) {
                        Toast.makeText(requireContext(),"Select tag",Toast.LENGTH_SHORT).show();
                        return;
                    }
                    RadioButton rb = dialogView.findViewById(sel);
                    String tag = rb.getText().toString();

                    List<String> cur = CategoryManager.getOrderedCategories(requireContext());
                    if (cur.contains(name)) {
                        Toast.makeText(requireContext(),"Exists",Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CategoryManager.saveCategoryWithTag(requireContext(), name, tag);

                    reloadCategories(name);
                    lastSelectedCategory = name;

                    Toast.makeText(requireContext(),"Added \""+name+"\" ("+tag+")",Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditCategoryDialog() {
        List<String> editable = new ArrayList<>();
        for (String c : CategoryManager.getOrderedCategories(requireContext())) {
            if (!CategoryManager.isDefaultCategory(c)) editable.add(c);
        }
        if (editable.isEmpty()) {
            Toast.makeText(requireContext(),"No editable categories",Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.sort(editable,String.CASE_INSENSITIVE_ORDER);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Tag")
                .setItems(editable.toArray(new String[0]), (d,w)-> showTagChangeDialog(editable.get(w)))
                .show();
    }

    private void showTagChangeDialog(String cat) {
        View dv = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_add_category_tagged, null);

        RadioGroup rg = dv.findViewById(R.id.radio_tag_group);
        EditText name = dv.findViewById(R.id.edit_category_name);

        name.setText(cat);
        name.setEnabled(false);

        String currentTag = CategoryManager.getTagForCategory(requireContext(), cat);

        if (currentTag.equals("Fixed")) rg.check(R.id.radio_fixed);
        else if (currentTag.equals("Necessity")) rg.check(R.id.radio_basic);
        else rg.check(R.id.radio_discretionary);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Tag")
                .setView(dv)
                .setPositiveButton("Save", (d,w)->{
                    int selectedId = rg.getCheckedRadioButtonId();
                    RadioButton rb = dv.findViewById(selectedId);
                    String newTag = rb.getText().toString();

                    CategoryManager.saveCategoryWithTag(requireContext(), cat, newTag);

                    lastSelectedCategory = cat;
                    reloadCategories(cat);

                    Toast.makeText(requireContext(),"Updated "+cat+" → "+newTag,Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteCategoryDialog() {

        List<String> deletable = new ArrayList<>();
        for (String c : CategoryManager.getOrderedCategories(requireContext())) {
            if (!CategoryManager.isDefaultCategory(c)) deletable.add(c);
        }
        if (deletable.isEmpty()) {
            Toast.makeText(requireContext(),"Nothing to delete",Toast.LENGTH_SHORT).show();
            return;
        }

        Collections.sort(deletable, String.CASE_INSENSITIVE_ORDER);

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setItems(deletable.toArray(new String[0]), (d,w)->{
                    String toRemove = deletable.get(w);

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Delete")
                            .setMessage("Delete \""+toRemove+"\" permanently?")
                            .setPositiveButton("Delete", (dd, ww)->{
                                CategoryManager.removeCategory(requireContext(), toRemove);
                                List<String> fresh = CategoryManager.getOrderedCategories(requireContext());
                                String desired = fresh.contains(lastSelectedCategory)
                                        ? lastSelectedCategory
                                        : "Groceries";

                                reloadCategories(desired);
                                lastSelectedCategory = desired;

                                Toast.makeText(requireContext(),"Deleted \""+toRemove+"\"",Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }
}
