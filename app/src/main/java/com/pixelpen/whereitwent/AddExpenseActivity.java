package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageButton;
import android.widget.AdapterView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private ImageButton btnFilter;

    private Spinner spinnerCategory;
    private EditText editDescription, editAmount;
    private TextView textDate;
    private Button btnSave;

    private String currentCategoryTag;
    private Expense editingExpense = null;
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categories = new ArrayList<>();
    private boolean suppressSpinnerCallback = false;
    private String lastSelectedCategory = "Groceries";

    private TextView textCategoryTag;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        drawerLayout = findViewById(R.id.drawer_layout);
        btnFilter = findViewById(R.id.btn_filter);

        spinnerCategory = findViewById(R.id.spinner_category);
        editDescription = findViewById(R.id.edit_description);
        editAmount = findViewById(R.id.edit_amount);
        textDate = findViewById(R.id.text_date);
        btnSave = findViewById(R.id.btn_save);
        textCategoryTag = findViewById(R.id.text_category_tag);

        loadCategoriesIntoSpinner("Groceries");

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (suppressSpinnerCallback) return;
                String selected = categories.get(position);
                if (selected.equals("➕ Manage Categories")) {
                    showManageCategoriesDialog();
                    return;
                }
                if (!selected.equals("⋯")) {
                    currentCategoryTag = selected;
                    lastSelectedCategory = selected;
                    updateCategoryTagLine(selected);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                currentCategoryTag = null;
                textCategoryTag.setText("➤ (auto insert)");
            }
        });

        btnFilter.setOnClickListener(v -> {
            if (drawerLayout != null) {
                drawerLayout.openDrawer(GravityCompat.END);
            }
        });

        textDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, day) -> {
                        String formatted = String.format(Locale.ENGLISH,
                                "%02d %s %04d",
                                day,
                                getMonthAbbreviation(month),
                                year);
                        textDate.setText(formatted);
                    },
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));
            dialog.show();
        });

        int expenseId = getIntent().getIntExtra("expense_id", -1);
        if (expenseId != -1) {
            editingExpense = ExpenseDatabase.getDatabase(this).expenseDao().getById(expenseId);
            if (editingExpense != null) {
                editDescription.setText(editingExpense.description);
                editAmount.setText(String.valueOf(editingExpense.amount));
                textDate.setText(editingExpense.date);
                int pos = -1;
                if (editingExpense.category != null) {
                    pos = categories.indexOf(editingExpense.category);
                }
                if (pos >= 0) {
                    suppressSpinnerCallback = true;
                    spinnerCategory.setSelection(pos);
                    suppressSpinnerCallback = false;
                    currentCategoryTag = editingExpense.category;
                    lastSelectedCategory = editingExpense.category;
                    updateCategoryTagLine(editingExpense.category);
                }
            }
        } else {
            Calendar today = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
            textDate.setText(sdf.format(today.getTime()));
            updateCategoryTagLine("Groceries");
        }

        btnSave.setOnClickListener(v -> {
            String description = editDescription.getText().toString().trim();
            String amountStr = editAmount.getText().toString().trim();
            String date = textDate.getText().toString();
            String category = spinnerCategory.getSelectedItem().toString();

            if (category.equals("➕ Manage Categories") || category.equals("⋯")) {
                Toast.makeText(this, "Please select a valid category", Toast.LENGTH_SHORT).show();
                return;
            }

            if (description.isEmpty() || amountStr.isEmpty() || date.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);
            ExpenseDatabase db = ExpenseDatabase.getDatabase(this);

            String finalCategory = (currentCategoryTag != null && !currentCategoryTag.isEmpty())
                    ? currentCategoryTag
                    : category;

            if (editingExpense != null) {
                editingExpense.description = description;
                editingExpense.amount = amount;
                editingExpense.date = date;
                editingExpense.category = finalCategory;
                db.expenseDao().update(editingExpense);
                Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
            } else {
                Expense newExpense = new Expense();
                newExpense.category = finalCategory;
                newExpense.date = date;
                newExpense.description = description;
                newExpense.amount = amount;
                db.expenseDao().insert(newExpense);
                Toast.makeText(this, "Expense added", Toast.LENGTH_SHORT).show();
            }

            Intent intent = new Intent(this, MainActivity.class);
            startActivity(intent);
            finish();
        });

        findViewById(R.id.btnViewAll).setOnClickListener(v -> startActivity(new Intent(this, ViewAllActivity.class)));
        findViewById(R.id.btnDateWise).setOnClickListener(v -> startActivity(new Intent(this, DateWiseActivity.class)));
        findViewById(R.id.btnMonthWise).setOnClickListener(v -> startActivity(new Intent(this, MonthWiseActivity.class)));
        findViewById(R.id.btnCategoryWise).setOnClickListener(v -> startActivity(new Intent(this, CategoryWiseActivity.class)));
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.END)) {
            drawerLayout.closeDrawer(GravityCompat.END);
            return;
        }
        super.onBackPressed();
    }

    private void loadCategoriesIntoSpinner(String desiredSelection) {
        List<String> ordered = CategoryManager.getOrderedCategories(this);

        List<String> defaults = new ArrayList<>();
        defaults.add("Groceries");
        defaults.add("Rent");
        defaults.add("Utilities");
        defaults.add("Bills");
        defaults.add("Transport");
        defaults.add("Other");

        List<String> custom = new ArrayList<>();
        for (String c : ordered) {
            if (!defaults.contains(c)) custom.add(c);
        }
        Collections.sort(custom, String.CASE_INSENSITIVE_ORDER);

        categories.clear();
        categories.addAll(defaults);
        if (!custom.isEmpty()) {
            categories.add("⋯");
            categories.addAll(custom);
        }
        categories.add("➕ Manage Categories");

        categoryAdapter = new ArrayAdapter<>(this, R.layout.spinner_item_selected, categories);
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
            updateCategoryTagLine(sel);
        } else {
            textCategoryTag.setText("➤ (auto insert)");
        }
    }

    private void reloadCategories(String desiredSelection) {
        loadCategoriesIntoSpinner(desiredSelection);
    }

    private AlertDialog showManageCategoriesDialog() {
        String[] options = {"Add Category", "Edit Tag", "Delete Category", "Reset Defaults"};
        return new AlertDialog.Builder(this)
                .setTitle("Manage Categories")
                .setItems(options, (DialogInterface dialog, int which) -> {
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
                            CategoryManager.resetToDefault(this);
                            reloadCategories("Groceries");
                            lastSelectedCategory = "Groceries";
                            Toast.makeText(this, "Defaults restored", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showAddCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category_tagged, null);
        EditText input = dialogView.findViewById(R.id.edit_category_name);
        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.radio_tag_group);

        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, w) -> {
                    String newCat = input.getText().toString().trim();
                    if (newCat.isEmpty()) {
                        Toast.makeText(this, "Enter category name", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    if (selectedId == -1) {
                        Toast.makeText(this, "Select a tag", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    android.widget.RadioButton selectedButton = dialogView.findViewById(selectedId);
                    String selectedTag = selectedButton.getText().toString();

                    List<String> current = CategoryManager.getOrderedCategories(this);
                    if (current.contains(newCat)) {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    CategoryManager.saveCategoryWithTag(this, newCat, selectedTag);
                    lastSelectedCategory = newCat;
                    reloadCategories(newCat);
                    Toast.makeText(this, "Added \"" + newCat + "\" (" + selectedTag + ")", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditCategoryDialog() {
        List<String> editable = new ArrayList<>();
        for (String c : CategoryManager.getOrderedCategories(this)) {
            if (!CategoryManager.isDefaultCategory(c)) editable.add(c);
        }
        if (editable.isEmpty()) {
            Toast.makeText(this, "No editable categories found", Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.sort(editable, String.CASE_INSENSITIVE_ORDER);
        String[] cats = editable.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Edit Tag")
                .setItems(cats, (dialog, which) -> showTagChangeDialog(cats[which]))
                .show();
    }

    private void showTagChangeDialog(String categoryName) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category_tagged, null);
        android.widget.RadioGroup radioGroup = dialogView.findViewById(R.id.radio_tag_group);
        EditText editName = dialogView.findViewById(R.id.edit_category_name);

        editName.setText(categoryName);
        editName.setEnabled(false);

        String currentTag = CategoryManager.getTagForCategory(this, categoryName);
        if (currentTag.equals("Fixed")) radioGroup.check(R.id.radio_fixed);
        else if (currentTag.equals("Basic")) radioGroup.check(R.id.radio_basic);
        else radioGroup.check(R.id.radio_discretionary);

        new AlertDialog.Builder(this)
                .setTitle("Change Tag")
                .setView(dialogView)
                .setPositiveButton("Save", (d, w) -> {
                    int selectedId = radioGroup.getCheckedRadioButtonId();
                    android.widget.RadioButton selected = dialogView.findViewById(selectedId);
                    String newTag = selected.getText().toString();

                    CategoryManager.saveCategoryWithTag(this, categoryName, newTag);
                    lastSelectedCategory = categoryName;
                    reloadCategories(categoryName);
                    Toast.makeText(this, "Updated tag for \"" + categoryName + "\" → " + newTag, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteCategoryDialog() {
        List<String> deletable = new ArrayList<>();
        for (String c : CategoryManager.getOrderedCategories(this)) {
            if (!CategoryManager.isDefaultCategory(c)) deletable.add(c);
        }
        if (deletable.isEmpty()) {
            Toast.makeText(this, "No deletable categories found", Toast.LENGTH_SHORT).show();
            return;
        }
        Collections.sort(deletable, String.CASE_INSENSITIVE_ORDER);
        String[] cats = deletable.toArray(new String[0]);

        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setItems(cats, (d, which) -> {
                    String toRemove = cats[which];
                    new AlertDialog.Builder(this)
                            .setTitle("Confirm Delete")
                            .setMessage("Delete \"" + toRemove + "\" permanently?")
                            .setPositiveButton("Delete", (dd, w) -> {
                                CategoryManager.removeCategory(this, toRemove);
                                List<String> fresh = CategoryManager.getOrderedCategories(this);
                                String desired = fresh.contains(lastSelectedCategory) ? lastSelectedCategory : "Groceries";
                                reloadCategories(desired);
                                if (!desired.equals("Groceries")) lastSelectedCategory = desired;
                                else lastSelectedCategory = "Groceries";
                                Toast.makeText(this, "Deleted \"" + toRemove + "\"", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }

    private void updateCategoryTagLine(String category) {
        if (textCategoryTag == null) return;
        String tag = CategoryManager.getTagForCategory(this, category);
        if (tag == null || tag.trim().isEmpty()) {
            textCategoryTag.setText("➤ (auto insert)");
        } else {
            textCategoryTag.setText("➤ " + tag);
            textCategoryTag.setTypeface(textCategoryTag.getTypeface(), android.graphics.Typeface.BOLD);
        }
    }

    private String getMonthAbbreviation(int monthIndex) {
        String[] months = {"Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.",
                "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."};
        return months[monthIndex];
    }
}
