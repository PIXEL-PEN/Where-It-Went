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
        inputItem       = v.findViewById(R.id.input_item);
        inputAmount     = v.findViewById(R.id.input_amount);
        textDate        = v.findViewById(R.id.text_date);
        textTag         = v.findViewById(R.id.text_tag);
        btnSave         = v.findViewById(R.id.btn_ok);

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
                textTag.setText("(auto insert)");
            }
        });
    }

    private void loadCategoriesIntoSpinner(String desiredSelection) {

        List<String> ordered = CategoryManager.getOrderedCategories(requireContext());

        List<String> defaults = Arrays.asList(
                "Groceries","Rent","Utilities","Bills","Transport","Other"
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
            String item     = inputItem.getText().toString();
            String amountStr= inputAmount.getText().toString();
            String uiDate   = textDate.getText().toString();

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
                editingExpense.category    = category;
                editingExpense.description = item;
                editingExpense.amount      = amount;
                editingExpense.date        = iso;
                dao.update(editingExpense);

                if (getActivity() instanceof DayDetailActivity) {
                    ((DayDetailActivity) getActivity()).refreshAfterEdit();
                }
            }
            else {
                Expense e = new Expense();
                e.category    = category;
                e.description = item;
                e.amount      = amount;
                e.date        = iso;
                dao.insert(e);

                if (getActivity() instanceof MainScreen) {
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
    // Category management placeholders
    // (unchanged — your original implementations remain)
    // --------------------------------------------------------------------
    private AlertDialog showManageCategoriesDialog() { return null; }
    private void showAddCategoryDialog() { }
    private void showEditCategoryDialog() { }
    private void showTagChangeDialog(String cat) { }
    private void showDeleteCategoryDialog() { }
}
