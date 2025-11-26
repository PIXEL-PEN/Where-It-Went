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

    private String lastSelectedCategory = "Groceries";
    private boolean suppressSpin = false;

    private ArrayAdapter<String> adapter;
    private final List<String> categories = new ArrayList<>();

    private Integer editingId = null;
    private Expense editingExpense = null;

    public static AddExpenseDialog newInstance(int id) {
        AddExpenseDialog d = new AddExpenseDialog();
        Bundle b = new Bundle();
        b.putInt("expense_id", id);
        d.setArguments(b);
        return d;
    }

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

        if (getArguments() != null)
            editingId = getArguments().getInt("expense_id", -1);

        setupSpinner();

        if (editingId != null && editingId > 0) {
            loadForEdit(editingId);
        } else {
            setDefaultDate();
        }

        setupDatePicker();
        setupSaveButton();

        return new AlertDialog.Builder(requireContext())
                .setView(v)
                .setCancelable(true)
                .create();
    }

    private void setupSpinner() {
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

        adapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_selected,
                categories
        );
        adapter.setDropDownViewResource(R.layout.spinner_item_dropdown);
        spinnerCategory.setAdapter(adapter);

        int idx = categories.indexOf(lastSelectedCategory);
        if (idx < 0) idx = 0;

        suppressSpin = true;
        spinnerCategory.setSelection(idx);
        suppressSpin = false;

        updateTagLine(lastSelectedCategory);

        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                if (suppressSpin) return;

                String sel = categories.get(pos);

                if (sel.equals("➕ Manage Categories")) {
                    showManageCategoriesDialog();
                    return;
                }

                if (!sel.equals("⋯")) {
                    lastSelectedCategory = sel;
                    updateTagLine(sel);
                }
            }

            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void updateTagLine(String category) {
        String tag = CategoryManager.getTagForCategory(requireContext(), category);
        if (tag == null || tag.trim().isEmpty())
            textTag.setText("(auto insert)");
        else
            textTag.setText(tag);
    }

    private void setDefaultDate() {
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        textDate.setText(sdf.format(c.getTime()));
    }

    private void loadForEdit(int id) {
        ExpenseDao dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao();
        editingExpense = dao.getById(id);
        if (editingExpense == null) return;

        inputItem.setText(editingExpense.description);
        inputAmount.setText(String.valueOf(editingExpense.amount));
        textDate.setText(DateUtils.isoToUi(editingExpense.date));

        int idx = categories.indexOf(editingExpense.category);
        if (idx >= 0) {
            suppressSpin = true;
            spinnerCategory.setSelection(idx);
            suppressSpin = false;
            updateTagLine(editingExpense.category);
        }
    }

    private void setupDatePicker() {
        textDate.setOnClickListener(v -> {

            Calendar cal = Calendar.getInstance();

            DatePickerDialog dlg = new DatePickerDialog(requireContext(),
                    (view, yr, mo, day) -> {
                        String formatted = String.format(Locale.ENGLISH,
                                "%02d %s %04d",
                                day,
                                getMonthAbbrev(mo),
                                yr);
                        textDate.setText(formatted);
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH));

            dlg.show();
        });
    }

    private String getMonthAbbrev(int i) {
        String[] m = {"Jan","Feb","Mar","Apr","May","Jun",
                "Jul","Aug","Sep","Oct","Nov","Dec"};
        return m[i];
    }

    private void setupSaveButton() {

        btnSave.setOnClickListener(v -> {

            String cat  = spinnerCategory.getSelectedItem().toString();
            String item = inputItem.getText().toString();
            String amtS = inputAmount.getText().toString();
            String ui   = textDate.getText().toString();

            if (item.isEmpty() || amtS.isEmpty()) {
                Toast.makeText(requireContext(), "Enter item and amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amt = Double.parseDouble(amtS);
            String iso = DateUtils.toIso(ui);

            if (iso == null) {
                Toast.makeText(requireContext(), "Invalid date", Toast.LENGTH_SHORT).show();
                return;
            }

            ExpenseDao dao = ExpenseDatabase.getDatabase(requireContext()).expenseDao();

            if (editingExpense != null) {
                editingExpense.category = cat;
                editingExpense.description = item;
                editingExpense.amount = amt;
                editingExpense.date = iso;
                dao.update(editingExpense);
            } else {
                Expense e = new Expense();
                e.category = cat;
                e.description = item;
                e.amount = amt;
                e.date = iso;
                dao.insert(e);
            }

            // ⭐ Trigger month refresh
            MainScreen.pendingRefresh = true;

            dismiss();
        });
    }

    private void showManageCategoriesDialog() {
        String[] opts = {"Add Category", "Edit Tag", "Delete Category", "Reset Defaults"};

        new AlertDialog.Builder(requireContext())
                .setTitle("Manage Categories")
                .setItems(opts, (d,w)->{
                    if (w == 0) showAddCategoryDialog();
                    else if (w == 1) showEditCategoryDialog();
                    else if (w == 2) showDeleteCategoryDialog();
                    else {
                        CategoryManager.resetToDefault(requireContext());
                        setupSpinner();
                    }
                })
                .show();
    }

    private void showAddCategoryDialog() {
        View dv = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_add_category_tagged, null);

        EditText input = dv.findViewById(R.id.edit_category_name);
        RadioGroup rg  = dv.findViewById(R.id.radio_tag_group);

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Category")
                .setView(dv)
                .setPositiveButton("Add", (x,y)->{
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;

                    int sel = rg.getCheckedRadioButtonId();
                    if (sel == -1) return;

                    RadioButton rb = dv.findViewById(sel);
                    String tag = rb.getText().toString();

                    List<String> cur = CategoryManager.getOrderedCategories(requireContext());
                    if (cur.contains(name)) return;

                    CategoryManager.saveCategoryWithTag(requireContext(), name, tag);
                    lastSelectedCategory = name;
                    setupSpinner();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditCategoryDialog() {
        List<String> editable = new ArrayList<>();
        for (String c : CategoryManager.getOrderedCategories(requireContext())) {
            if (!CategoryManager.isDefaultCategory(c))
                editable.add(c);
        }

        if (editable.isEmpty()) return;

        Collections.sort(editable, String.CASE_INSENSITIVE_ORDER);

        new AlertDialog.Builder(requireContext())
                .setTitle("Edit Tag")
                .setItems(editable.toArray(new String[0]), (d,w)->
                        showTagChangeDialog(editable.get(w)))
                .show();
    }

    private void showTagChangeDialog(String cat) {
        View dv = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_add_category_tagged, null);

        RadioGroup rg = dv.findViewById(R.id.radio_tag_group);
        EditText name = dv.findViewById(R.id.edit_category_name);

        name.setText(cat);
        name.setEnabled(false);

        String tag = CategoryManager.getTagForCategory(requireContext(), cat);

        if (tag.equals("Fixed")) rg.check(R.id.radio_fixed);
        else if (tag.equals("Necessity")) rg.check(R.id.radio_basic);
        else rg.check(R.id.radio_discretionary);

        new AlertDialog.Builder(requireContext())
                .setTitle("Change Tag")
                .setView(dv)
                .setPositiveButton("Save", (x,y)->{
                    int sel = rg.getCheckedRadioButtonId();
                    RadioButton rb = dv.findViewById(sel);
                    String t = rb.getText().toString();

                    CategoryManager.saveCategoryWithTag(requireContext(), cat, t);

                    lastSelectedCategory = cat;
                    setupSpinner();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteCategoryDialog() {
        List<String> del = new ArrayList<>();
        for (String c : CategoryManager.getOrderedCategories(requireContext())) {
            if (!CategoryManager.isDefaultCategory(c))
                del.add(c);
        }

        if (del.isEmpty()) return;

        Collections.sort(del, String.CASE_INSENSITIVE_ORDER);

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setItems(del.toArray(new String[0]), (d,w)->{

                    String rm = del.get(w);

                    new AlertDialog.Builder(requireContext())
                            .setTitle("Confirm Delete")
                            .setMessage("Delete \""+rm+"\" permanently?")
                            .setPositiveButton("Delete", (x,y)->{

                                CategoryManager.removeCategory(requireContext(), rm);
                                setupSpinner();

                            })
                            .setNegativeButton("Cancel", null)
                            .show();
                })
                .show();
    }
}
