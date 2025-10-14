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

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class AddExpenseActivity extends AppCompatActivity {

    private Spinner spinnerCategory;
    private EditText editDescription, editAmount;
    private TextView textDate;
    private Button btnSave;

    private Expense editingExpense = null;
    private ArrayAdapter<String> categoryAdapter;
    private List<String> categories;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_expense);

        spinnerCategory = findViewById(R.id.spinner_category);
        editDescription = findViewById(R.id.edit_description);
        editAmount = findViewById(R.id.edit_amount);
        textDate = findViewById(R.id.text_date);
        btnSave = findViewById(R.id.btn_save);

        List<String> defaults = new ArrayList<>();
        defaults.add("Groceries");
        defaults.add("Rent");
        defaults.add("Utilities");
        defaults.add("Bills");
        defaults.add("Transport");
        defaults.add("Other");

        List<String> allFromManager = CategoryManager.getOrderedCategories(this);
        List<String> custom = new ArrayList<>();
        for (String c : allFromManager) {
            if (!defaults.contains(c)) custom.add(c);
        }

        categories = new ArrayList<>();
        categories.addAll(defaults);
        if (!custom.isEmpty()) {
            categories.add("⋯");
            categories.addAll(custom);
        }
        categories.add("➕ Manage Categories");

        categoryAdapter = new ArrayAdapter<String>(
                this,
                R.layout.spinner_item_selected,
                categories
        ) {
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
                if (item.equals("⋯")) {
                    tv.setTextColor(0xFF777777);
                } else {
                    tv.setTextColor(0xFF000000);
                }
                return view;
            }
        };

        categoryAdapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCategory.setAdapter(categoryAdapter);

        spinnerCategory.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position == categories.size() - 1) {
                    showManageCategoriesDialog();
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        textDate.setOnClickListener(v -> {
            final Calendar calendar = Calendar.getInstance();
            DatePickerDialog dialog = new DatePickerDialog(this,
                    (view, year, month, dayOfMonth) -> {
                        String formatted = String.format(Locale.ENGLISH,
                                "%02d %s %04d",
                                dayOfMonth,
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

                int pos = categoryAdapter.getPosition(editingExpense.category);
                if (pos >= 0) spinnerCategory.setSelection(pos);
            }
        } else {
            Calendar today = Calendar.getInstance();
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM. yyyy", Locale.ENGLISH);
            textDate.setText(sdf.format(today.getTime()));
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

            if (editingExpense != null) {
                editingExpense.description = description;
                editingExpense.amount = amount;
                editingExpense.date = date;
                editingExpense.category = category;
                db.expenseDao().update(editingExpense);
                Toast.makeText(this, "Expense updated", Toast.LENGTH_SHORT).show();
            } else {
                Expense newExpense = new Expense();
                newExpense.category = category;
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
    }

    private void showManageCategoriesDialog() {
        String[] options = {"Add Category", "Delete Category", "Reset Defaults"};
        new AlertDialog.Builder(this)
                .setTitle("Manage Categories")
                .setItems(options, (DialogInterface dialog, int which) -> {
                    if (which == 0) {
                        showAddCategoryDialog();
                    } else if (which == 1) {
                        showDeleteCategoryDialog();
                    } else {
                        CategoryManager.resetToDefault(this);
                        reloadCategories();
                    }
                })
                .show();
    }

    private void showAddCategoryDialog() {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_category, null);

        EditText input = dialogView.findViewById(R.id.edit_category_name);
        Spinner tagSpinner = dialogView.findViewById(R.id.spinner_tag);

        String[] tags = {"Fixed", "Basic", "Discretionary"};
        ArrayAdapter<String> tagAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, tags);
        tagSpinner.setAdapter(tagAdapter);

        new AlertDialog.Builder(this)
                .setTitle("Add Category")
                .setView(dialogView)
                .setPositiveButton("Add", (dialog, which) -> {
                    String newCat = input.getText().toString().trim();
                    String selectedTag = tagSpinner.getSelectedItem().toString();

                    if (newCat.isEmpty()) {
                        Toast.makeText(this, "Enter category name", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (categories.contains(newCat)) {
                        Toast.makeText(this, "Category already exists", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    // ✅ Add new category just before “Manage Categories” item
                    categories.add(categories.size() - 1, newCat);

                    // ✅ Persist using the new unified method
                    CategoryManager.saveCategoryWithTag(this, newCat, selectedTag);

                    // ✅ Refresh adapter
                    categoryAdapter.notifyDataSetChanged();

                    Toast.makeText(this,
                            "Added \"" + newCat + "\" (" + selectedTag + ")", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();

    }

    private void showDeleteCategoryDialog() {
        String[] cats = categories.subList(0, categories.size() - 1).toArray(new String[0]);
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setItems(cats, (d, which) -> {
                    String toRemove = cats[which];
                    categories.remove(toRemove);

                    // ✅ Re-persist categories using new tag logic
                    for (String name : categories.subList(0, categories.size() - 1)) {
                        String tag = CategoryManager.isDefaultCategory(name)
                                ? "Fixed"
                                : CategoryManager.getTagForCategory(this, name);
                        CategoryManager.saveCategoryWithTag(this, name, tag);
                    }

                    categoryAdapter.notifyDataSetChanged();
                })
                .show();
    }

    private void reloadCategories() {
        categories = new ArrayList<>(CategoryManager.getOrderedCategories(this));
        categories.add("➕ Manage Categories");
        categoryAdapter.clear();
        categoryAdapter.addAll(categories);
        categoryAdapter.notifyDataSetChanged();
    }

    private String getMonthAbbreviation(int monthIndex) {
        String[] months = {"Jan.", "Feb.", "Mar.", "Apr.", "May", "Jun.",
                "Jul.", "Aug.", "Sep.", "Oct.", "Nov.", "Dec."};
        return months[monthIndex];
    }
}
