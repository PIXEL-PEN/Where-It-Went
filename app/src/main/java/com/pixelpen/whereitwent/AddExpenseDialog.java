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
import android.text.InputType;

import android.content.DialogInterface;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.ViewGroup;

import android.content.SharedPreferences;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import android.content.Intent;
import android.widget.Button;
import android.widget.TextView;

import java.util.concurrent.Executors;



public class AddExpenseDialog extends DialogFragment {

    private static class Account {
        String name;
        String type;

        Account(String n, String t) {
            name = n;
            type = t;
        }
    }

// ======================================================
// METHOD MAP — AddExpenseDialog
// ======================================================
//
// ACCOUNT CORE
//   - addAccount(String name, String type)
//   - saveAccounts()
//   - loadAccounts()
//   - accountNameExists(String name)
//
// ACCOUNT UI
//   - showAddAccountDialog()
//   - setupAccountSpinner()
//   - populateAccountCategoriesFor(String accountName)
//
// ACCOUNT CATEGORY
//   - showAddAccountCategoryDialog()
//   - showRenameAccountCategoryDialog()
//   - showDeleteAccountCategoryDialog()
//
// HELPERS
//   - findAccountByName(String name)
//   - getDefaultCategoriesFor(Account acc)
//   - restoreLastAccountSelection()
//
// ======================================================


    private final List<Account> accounts = new ArrayList<>();

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

    private LinearLayout containerAccounts;
    private TextView textNoAccounts;


    private Spinner spinnerAccount;
    private Spinner spinnerAccountCategory;

    private ArrayAdapter<String> accountAdapter;
    private ArrayAdapter<String> accountCategoryAdapter;

    private final List<String> accountItems = new ArrayList<>();
    private final List<String> accountCategories = new ArrayList<>();

    private String lastValidAccount = null;
    private Calendar accountDate;


    private final Map<String, List<String>> accountCustomCategories = new HashMap<>();


    private static final String PREF_ACCOUNTS = "accounts_store";
    private static final String KEY_ACCOUNTS = "accounts";

    private static final String PREF_ACCOUNT_CATEGORIES = "account_categories_store";
    private static final String KEY_ACCOUNT_CATEGORIES = "account_categories";

    private static final String PREFS_UI = "ui_prefs";
    private static final String KEY_LAST_ACTIVE = "last_active_module";


    private EditText editAccountItem;
    private EditText editAccountAmount;
    private EditText editAccountNote;

    private static final String ARG_MANAGE_ONLY = "manage_only";



    public static AddExpenseDialog newInstance(int expenseId) {
        AddExpenseDialog d = new AddExpenseDialog();
        Bundle b = new Bundle();
        b.putInt("expense_id", expenseId);
        d.setArguments(b);
        return d;
    }

    public static AddExpenseDialog newManageAccountsInstance() {
        AddExpenseDialog d = new AddExpenseDialog();
        Bundle b = new Bundle();
        b.putBoolean(ARG_MANAGE_ONLY, true);
        d.setArguments(b);
        return d;
    }




    private enum ActiveModule {
        DAILY,
        ACCOUNTS
    }




    private ActiveModule activeModule = ActiveModule.DAILY;


    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        Dialog dialog = new Dialog(requireContext());
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new ColorDrawable(Color.TRANSPARENT));
        }

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_add_expense, null);


        accountDate = Calendar.getInstance();
        accountDate.set(Calendar.HOUR_OF_DAY, 0);
        accountDate.set(Calendar.MINUTE, 0);
        accountDate.set(Calendar.SECOND, 0);
        accountDate.set(Calendar.MILLISECOND, 0);


        View dailyContainer = v.findViewById(R.id.card_daily);
        View accountsContainer = v.findViewById(R.id.card_other);

        SharedPreferences prefs =
                requireContext().getSharedPreferences(PREFS_UI, Context.MODE_PRIVATE);

        String saved = prefs.getString(KEY_LAST_ACTIVE, "DAILY");
        activeModule = "ACCOUNTS".equals(saved)
                ? ActiveModule.ACCOUNTS
                : ActiveModule.DAILY;


        applyActiveState(dailyContainer, accountsContainer);

        dailyContainer.setOnClickListener(vv -> {
            if (activeModule != ActiveModule.DAILY) {
                activeModule = ActiveModule.DAILY;
                saveActiveModule();
                applyActiveState(dailyContainer, accountsContainer);
            }
        });

        accountsContainer.setOnClickListener(vv -> {
            if (activeModule != ActiveModule.ACCOUNTS) {
                activeModule = ActiveModule.ACCOUNTS;
                saveActiveModule();
                applyActiveState(dailyContainer, accountsContainer);
            }
        });


        dailyContainer.setOnClickListener(vv -> {
            if (activeModule != ActiveModule.DAILY) {
                activeModule = ActiveModule.DAILY;
                applyActiveState(dailyContainer, accountsContainer);
            }
        });

        accountsContainer.setOnClickListener(vv -> {
            if (activeModule != ActiveModule.ACCOUNTS) {
                activeModule = ActiveModule.ACCOUNTS;
                applyActiveState(dailyContainer, accountsContainer);
            }
        });


        // -----------------------------
// -----------------------------
// ACCOUNTS — SUBMIT (REAL BUTTON)
// -----------------------------
        TextView submitAccount = v.findViewById(R.id.text_account_submit);

        if (submitAccount != null) {
            submitAccount.setOnClickListener(vv -> {

                android.util.Log.e("ACCOUNTS", "SUBMIT CLICKED");

                insertAccountExpense();

                dismiss();

                startActivity(new Intent(
                        requireContext(),
                        AccountsOverviewActivity.class
                ));
            });
        }

        TextView linkManage = v.findViewById(R.id.link_manage_accounts);
        if (linkManage != null) {
            linkManage.setOnClickListener(vv -> showManageAccountsDialog());
        }


        loadAccounts();

        loadAccountCategories();


        containerAccounts = v.findViewById(R.id.container_account_entry);
        textNoAccounts = v.findViewById(R.id.text_no_accounts);

        updateAccountsVisibility();


        // -----------------------------
        // ACCOUNTS — DATE (SUBMIT ROW)
        // -----------------------------
        TextView textAccountDate = v.findViewById(R.id.text_account_date);
        if (textAccountDate != null) {
            // Force authoritative value – never trust layout/default text
            updateAccountDateLabel(textAccountDate);

            textAccountDate.setOnClickListener(vv ->
                    showAccountDatePicker(textAccountDate));
        }

        TextView addAccount = v.findViewById(R.id.text_add_account);
        addAccount.setOnClickListener(vv -> showAddAccountDialog());

        containerAccounts = v.findViewById(R.id.container_account_entry);
        textNoAccounts = v.findViewById(R.id.text_no_accounts);

        // -----------------------------
        // DAILY LIVING (unchanged)
        // -----------------------------
        spinnerCategory = v.findViewById(R.id.spinner_category);

        inputItem = v.findViewById(R.id.input_item);
        inputItem.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        inputAmount = v.findViewById(R.id.input_amount);

        textDate = v.findViewById(R.id.text_date);
        textTag = v.findViewById(R.id.text_tag);
        btnSave = v.findViewById(R.id.btn_ok);

        // -----------------------------
        // ACCOUNTS — INIT ADAPTERS ONCE
        // -----------------------------
        spinnerAccount = v.findViewById(R.id.spinner_account_project);
        spinnerAccountCategory = v.findViewById(R.id.spinner_account_category);

        accountAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_account_selected,
                accountItems
        );
        accountAdapter.setDropDownViewResource(
                R.layout.spinner_item_account_dropdown
        );
        spinnerAccount.setAdapter(accountAdapter);

        accountCategoryAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_account_selected,
                accountCategories
        );
        accountCategoryAdapter.setDropDownViewResource(
                R.layout.spinner_item_account_dropdown
        );
        spinnerAccountCategory.setAdapter(accountCategoryAdapter);

        spinnerAccountCategory.setOnLongClickListener(view -> {


            String accountName = lastValidAccount;
            if (accountName == null) return true;

            int pos = spinnerAccountCategory.getSelectedItemPosition();
            if (pos < 0 || pos >= accountCategories.size()) return true;

            String selected = accountCategories.get(pos);

            // Block defaults
            Account acc = findAccountByName(accountName);
            if (acc != null && getDefaultCategoriesFor(acc).contains(selected)) {
                return true;
            }

            showAccountCategoryOptionsDialog(accountName, selected);
            return true;
        });


        ImageButton btnAddAccountCategory =
                v.findViewById(R.id.btn_add_account_category);
        btnAddAccountCategory.setOnClickListener(vv ->
                showAddAccountCategoryDialog());


        // -----------------------------
        // POPULATE SPINNERS
        // -----------------------------
        setupAccountSpinner();
        setupAccountCategorySpinner();

        // -----------------------------
        // ACCOUNTS — INPUT BINDING
        // -----------------------------
        editAccountItem = v.findViewById(R.id.input_account_item);
        editAccountAmount = v.findViewById(R.id.input_account_amount);
        editAccountNote = v.findViewById(R.id.input_account_note);

        // -----------------------------
        // EDIT / NEW LOGIC (unchanged)
        // -----------------------------
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

        if (getArguments() != null
                && getArguments().getBoolean("open_manage_categories", false)) {
            v.postDelayed(() -> showManageCategoriesDialog(), 100);
        }

        dialog.setContentView(v);
        dialog.setCancelable(true);
        return dialog;
    }


    private void updateAccountsVisibility() {

        if (accounts.isEmpty()) {
            textNoAccounts.setVisibility(View.VISIBLE);
            containerAccounts.setVisibility(View.GONE);
        } else {
            textNoAccounts.setVisibility(View.GONE);
            containerAccounts.setVisibility(View.VISIBLE);
        }
    }


    private void saveAccounts() {

        try {
            JSONArray arr = new JSONArray();

            for (Account a : accounts) {
                JSONObject obj = new JSONObject();
                obj.put("name", a.name);
                obj.put("type", a.type);
                arr.put(obj);
            }

            SharedPreferences prefs =
                    requireContext().getSharedPreferences(
                            PREF_ACCOUNTS,
                            Context.MODE_PRIVATE);

            prefs.edit()
                    .putString(KEY_ACCOUNTS, arr.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }

    private void saveAccountCategories() {

        try {
            JSONObject root = new JSONObject();

            for (Map.Entry<String, List<String>> e : accountCustomCategories.entrySet()) {
                JSONArray arr = new JSONArray();
                for (String c : e.getValue()) {
                    arr.put(c);
                }
                root.put(e.getKey(), arr);
            }

            SharedPreferences prefs =
                    requireContext().getSharedPreferences(
                            PREF_ACCOUNT_CATEGORIES,
                            Context.MODE_PRIVATE);

            prefs.edit()
                    .putString(KEY_ACCOUNT_CATEGORIES, root.toString())
                    .apply();

        } catch (Exception ignored) {
        }
    }


    private void loadAccountCategories() {

        accountCustomCategories.clear();

        SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        PREF_ACCOUNT_CATEGORIES,
                        Context.MODE_PRIVATE);

        String raw = prefs.getString(KEY_ACCOUNT_CATEGORIES, null);
        if (raw == null) return;

        try {
            JSONObject root = new JSONObject(raw);
            Iterator<String> keys = root.keys();

            while (keys.hasNext()) {
                String account = keys.next();
                JSONArray arr = root.getJSONArray(account);

                List<String> list = new ArrayList<>();
                for (int i = 0; i < arr.length(); i++) {
                    list.add(arr.getString(i));
                }

                accountCustomCategories.put(account, list);
            }

        } catch (Exception ignored) {
        }
    }


    private void loadAccounts() {

        accounts.clear();

        List<AccountEntity> rows =
                ExpenseDatabase.getDatabase(requireContext())
                        .accountDao()
                        .getActiveAccounts();   // archived = false only

        for (AccountEntity e : rows) {
            accounts.add(new Account(e.name, e.type));
        }
    }


    private void showAddAccountCategoryDialog() {

        String accountName = lastValidAccount;
        if (accountName == null) return;

        final EditText input = new EditText(requireContext());
        input.setHint("Category name");
        input.setInputType(
                InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
        );

        new AlertDialog.Builder(requireContext())
                .setTitle("Add Category")
                .setView(input)
                .setPositiveButton("Add", (d, w) -> {

                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) return;

                    List<String> custom =
                            accountCustomCategories.computeIfAbsent(
                                    accountName, k -> new ArrayList<>());

                    String normalized = name.toLowerCase(Locale.ENGLISH);

// Block duplicates vs DEFAULTS
                    Account acc = findAccountByName(accountName);
                    if (acc != null) {
                        for (String def : getDefaultCategoriesFor(acc)) {
                            if (def.toLowerCase(Locale.ENGLISH).equals(normalized)) {
                                Toast.makeText(requireContext(),
                                        "Category already exists",
                                        Toast.LENGTH_SHORT).show();
                                return;
                            }
                        }
                    }

// Block duplicates vs CUSTOM
                    for (String c : custom) {
                        if (c.toLowerCase(Locale.ENGLISH).equals(normalized)) {
                            Toast.makeText(requireContext(),
                                    "Category already exists",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }


                    custom.add(name);

                    saveAccountCategories();


                    populateAccountCategoriesFor(accountName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void updateAccountDateLabel(TextView tv) {
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
        tv.setText(sdf.format(accountDate.getTime()));
    }

    private void showAccountDatePicker(TextView tv) {

        DatePickerDialog dlg = new DatePickerDialog(
                requireContext(),
                (view, y, m, d) -> {
                    accountDate.set(y, m, d);
                    updateAccountDateLabel(tv);
                },
                accountDate.get(Calendar.YEAR),
                accountDate.get(Calendar.MONTH),
                accountDate.get(Calendar.DAY_OF_MONTH)
        );

        dlg.show();
    }

    private void showAccountCategoryOptionsDialog(
            String accountName,
            String category) {

        String[] options = {"Rename", "Delete", "Cancel"};

        new AlertDialog.Builder(requireContext())
                .setTitle(category)
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0:
                            showRenameAccountCategoryDialog(accountName, category);
                            break;

                    }
                })
                .show();
    }

    private void showRenameAccountCategoryDialog(
            String accountName,
            String oldName) {

        EditText input = new EditText(requireContext());
        input.setText(oldName);
        input.setSelection(oldName.length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Rename Category")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {

                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;

                    List<String> custom = accountCustomCategories.get(accountName);
                    if (custom == null) return;

                    if (custom.contains(newName)) return;

                    int idx = custom.indexOf(oldName);
                    if (idx >= 0) {
                        custom.set(idx, newName);

                        saveAccountCategories();


                    }

                    populateAccountCategoriesFor(accountName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showDeleteAccountCategoryDialog(
            String accountName,
            String category) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Category")
                .setMessage("Delete \"" + category + "\"?")
                .setPositiveButton("Delete", (d, w) -> {

                    List<String> custom = accountCustomCategories.get(accountName);
                    if (custom == null) return;

                    custom.remove(category);
                    saveAccountCategories();


                    populateAccountCategoriesFor(accountName);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void setupAccountSpinner() {

        accountItems.clear();

        List<Account> projects = new ArrayList<>();
        List<Account> travel = new ArrayList<>();
        List<Account> custom = new ArrayList<>();

        for (Account a : accounts) {
            if ("project".equals(a.type)) {
                projects.add(a);
            } else if ("travel".equals(a.type)) {
                travel.add(a);
            } else {
                custom.add(a);
            }
        }

        Comparator<Account> byName =
                Comparator.comparing(a -> a.name.toLowerCase(Locale.ENGLISH));

        Collections.sort(projects, byName);
        Collections.sort(travel, byName);
        Collections.sort(custom, byName);

        addAccountGroup(projects);
        addSeparatorIfNeeded(projects, travel);
        addAccountGroup(travel);
        addSeparatorIfNeeded(travel, custom);
        addAccountGroup(custom);

        accountAdapter.notifyDataSetChanged();

        spinnerAccount.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {

                String selected = accountItems.get(pos);

                if (isSeparator(selected)) {
                    restoreLastAccountSelection();
                    return;
                }

                lastValidAccount = selected;
                populateAccountCategoriesFor(lastValidAccount);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        // Default selection
        if (!accountItems.isEmpty()) {
            spinnerAccount.setSelection(0);
            lastValidAccount = accountItems.get(0);
            populateAccountCategoriesFor(lastValidAccount);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Dialog d = getDialog();
        if (d != null && d.getWindow() != null) {

            int width = (int) (getResources()
                    .getDisplayMetrics().widthPixels * 0.90);

            d.getWindow().setLayout(
                    width,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }

        boolean manageOnly =
                getArguments() != null &&
                        getArguments().getBoolean(ARG_MANAGE_ONLY, false);

        if (manageOnly) {

            getArguments().remove(ARG_MANAGE_ONLY);

            showManageAccountsDialog();

            if (d != null) {
                d.hide();   // ← important change
            }
        }
    }




    private void showAddAccountDialog() {

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_add_account, null);


        TextView linkManage = view.findViewById(R.id.link_manage_accounts);
        if (linkManage != null) {
            linkManage.setOnClickListener(vv -> {
                showManageAccountsDialog();
            });
        }


        EditText editName = view.findViewById(R.id.edit_account_name);
        RadioGroup radioType = view.findViewById(R.id.radio_account_type);
        TextView btnCancel = view.findViewById(R.id.btn_cancel);
        TextView btnOk = view.findViewById(R.id.btn_ok);


        AlertDialog dialog = new AlertDialog.Builder(getContext())
                .setView(view)
                .create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        btnOk.setOnClickListener(v -> {
            String name = editName.getText().toString().trim();
            if (name.isEmpty()) {
                editName.setError("Required");
                return;
            }

            if (accountNameExists(name)) {
                editName.setError("Account already exists");
                return;
            }


            int checked = radioType.getCheckedRadioButtonId();
            String type;

            if (checked == R.id.radio_project) type = "project";
            else if (checked == R.id.radio_travel) type = "travel";
            else type = "custom";

            addAccount(name, type);
            dialog.dismiss();
        });

        dialog.show();

        LinearLayout foreignLayout = view.findViewById(R.id.layout_foreign_currency);
        Switch foreignSwitch = view.findViewById(R.id.switch_foreign_currency);

        radioType.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_travel) {
                foreignLayout.setVisibility(View.VISIBLE);
            } else {
                foreignLayout.setVisibility(View.GONE);
                foreignSwitch.setChecked(false);
            }
        });
        foreignSwitch.setOnCheckedChangeListener((btn, isChecked) -> {
            if (isChecked) {
                showForeignCurrencyDialog();
            }
        });


    }


    private void showAccountActionsDialog(AccountEntity acc) {

        AccountDao dao =
                ExpenseDatabase.getDatabase(requireContext()).accountDao();

        String[] options;

        if (acc.archived) {
            options = new String[]{"Unarchive", "Delete", "Cancel"};
        } else {
            options = new String[]{"Rename", "Archive", "Delete", "Cancel"};
        }

        new AlertDialog.Builder(requireContext())
                .setTitle(acc.name)
                .setItems(options, (d, which) -> {

                    if (acc.archived) {
                        switch (which) {
                            case 0: // Unarchive
                                dao.setArchived(acc.id, false);
                                loadAccounts();
                                updateAccountsVisibility();
                                setupAccountSpinner();
                                Toast.makeText(requireContext(),
                                        "Account unarchived",
                                        Toast.LENGTH_SHORT).show();
                                break;

                            case 1: // Delete
                                showDeleteAccountDialog(acc);
                                break;
                        }
                    } else {
                        switch (which) {
                            case 0: // Rename
                                showRenameAccountDialog(
                                        new Account(acc.name, acc.type));
                                break;

                            case 1: // Archive
                                dao.setArchived(acc.id, true);
                                loadAccounts();
                                updateAccountsVisibility();
                                setupAccountSpinner();
                                Toast.makeText(requireContext(),
                                        "Account archived",
                                        Toast.LENGTH_SHORT).show();
                                break;

                            case 2: // Delete
                                showDeleteAccountDialog(acc);
                                break;
                        }
                    }
                })
                .show();
    }


    private void showRenameAccountDialog(Account acc) {

        EditText input = new EditText(requireContext());
        input.setText(acc.name);
        input.setSelection(acc.name.length());

        new AlertDialog.Builder(requireContext())
                .setTitle("Rename Account")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {

                    String newName = input.getText().toString().trim();
                    if (newName.isEmpty()) return;

                    if (accountNameExists(newName)) {
                        Toast.makeText(requireContext(),
                                "Account already exists",
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    AccountDao dao =
                            ExpenseDatabase.getDatabase(requireContext()).accountDao();

                    AccountEntity entity = dao.getAccountByName(acc.name);
                    if (entity == null) return;

                    entity.name = newName;
                    dao.update(entity);

                    loadAccounts();
                    updateAccountsVisibility();
                    setupAccountSpinner();

                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showManageAccountsDialog() {

        AccountDao dao =
                ExpenseDatabase.getDatabase(requireContext()).accountDao();

        List<AccountEntity> all = dao.getAllAccounts();

        if (all.isEmpty()) {
            Toast.makeText(requireContext(),
                    "No accounts",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] labels = new String[all.size()];
        for (int i = 0; i < all.size(); i++) {
            AccountEntity e = all.get(i);
            labels[i] = e.archived
                    ? e.name + " (archived)"
                    : e.name;
        }

        new AlertDialog.Builder(requireContext())
                .setTitle("Manage Accounts")
                .setItems(labels, (d, which) -> {
                    showAccountActionsDialog(all.get(which));
                })
                .setNegativeButton("Close", null)
                .show();
    }

    private void showDeleteAccountDialog(AccountEntity acc) {

        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Delete \"" + acc.name + "\"?\n\nThis cannot be undone.")
                .setPositiveButton("Delete", (d, w) -> {

                    AccountDao dao =
                            ExpenseDatabase.getDatabase(requireContext()).accountDao();

                    // Hard delete from Room
                    dao.delete(acc);

                    // Remove custom categories for this account
                    accountCustomCategories.remove(acc.name);
                    saveAccountCategories();

                    // Reload active accounts from Room
                    loadAccounts();

                    // Refresh UI
                    lastValidAccount = null;
                    updateAccountsVisibility();
                    setupAccountSpinner();
                    accountCategories.clear();
                    accountCategoryAdapter.notifyDataSetChanged();

                    Toast.makeText(requireContext(),
                            "Account deleted",
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }


    private void showForeignCurrencyDialog() {
        // placeholder — currency, rate, base currency
    }


    private void addAccount(String name, String type) {

        // 1. Insert into Room (source of truth)
        ExpenseDatabase.getDatabase(requireContext())
                .accountDao()
                .insert(new AccountEntity(name, type, false));

        // 2. Reload accounts from Room
        loadAccounts();

        // 3. Update UI
        updateAccountsVisibility();
        setupAccountSpinner();
    }


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

    private void setupSpinner() {

        loadCategoriesIntoSpinner("Groceries");
        spinnerCategory.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

                if (suppressSpinnerCallback) return;

                String selected = categories.get(position);

                if (selected.equals("➕ Manage Categories")) {

                    int prevIndex = categories.indexOf(lastSelectedCategory);
                    if (prevIndex < 0) prevIndex = 0;

                    suppressSpinnerCallback = true;
                    spinnerCategory.setSelection(prevIndex);
                    suppressSpinnerCallback = false;

                    showManageCategoriesDialog();
                    return;
                }

                if (selected.equals("⋯")) {

                    int prevIndex = categories.indexOf(lastSelectedCategory);
                    if (prevIndex < 0) prevIndex = 0;

                    suppressSpinnerCallback = true;
                    spinnerCategory.setSelection(prevIndex);
                    suppressSpinnerCallback = false;

                    return;
                }

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

                        Calendar todayCal = Calendar.getInstance();
                        todayCal.set(Calendar.HOUR_OF_DAY, 0);
                        todayCal.set(Calendar.MINUTE, 0);
                        todayCal.set(Calendar.SECOND, 0);
                        todayCal.set(Calendar.MILLISECOND, 0);

                        Calendar chosen = Calendar.getInstance();
                        chosen.set(yr, mo, day, 0, 0, 0);

                        if (chosen.after(todayCal)) {
                            Toast.makeText(requireContext(),
                                    "Future dates are not allowed",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

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

    private void addAccountGroup(List<Account> list) {
        for (Account a : list) {
            accountItems.add(a.name);
        }
    }

    private void addSeparatorIfNeeded(List<Account> a, List<Account> b) {
        if (!a.isEmpty() && !b.isEmpty()) {
            accountItems.add("—");
        }
    }

    private boolean isSeparator(String s) {
        return "—".equals(s);
    }

    private void restoreLastAccountSelection() {
        if (lastValidAccount == null) return;
        int idx = accountItems.indexOf(lastValidAccount);
        if (idx >= 0) spinnerAccount.setSelection(idx);
    }

    private void setupAccountCategorySpinner() {
        accountCategoryAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_account_selected,
                accountCategories
        );
        accountCategoryAdapter.setDropDownViewResource(
                R.layout.spinner_item_account_dropdown
        );
        spinnerAccountCategory.setAdapter(accountCategoryAdapter);

    }


    private void populateAccountCategoriesFor(String accountName) {

        accountCategories.clear();

        Account acc = findAccountByName(accountName);
        if (acc == null) return;

        // 1. DEFAULTS (by account TYPE)
        switch (acc.type) {

            case "travel":
                accountCategories.addAll(Arrays.asList(
                        "Lodging",
                        "Meals",
                        "Transportation",
                        "Activities",
                        "Other"
                ));
                break;

            case "project":
                accountCategories.addAll(Arrays.asList(
                        "Materials",
                        "Labor",
                        "Tools",
                        "Services",
                        "Other"
                ));
                break;

            default:
                accountCategories.add("Other");
                break;
        }

        // 2. CUSTOM (by ACCOUNT NAME)
        List<String> custom = accountCustomCategories.get(accountName);
        if (custom != null && !custom.isEmpty()) {
            accountCategories.addAll(custom);
        }

        // 3. REFRESH SPINNER
        accountCategoryAdapter.notifyDataSetChanged();
        spinnerAccountCategory.setSelection(0);
    }

    private List<String> getDefaultCategoriesFor(Account acc) {

        switch (acc.type) {

            case "travel":
                return Arrays.asList(
                        "Lodging",
                        "Meals",
                        "Transportation",
                        "Activities",
                        "Other"
                );

            case "project":
                return Arrays.asList(
                        "Materials",
                        "Labor",
                        "Tools",
                        "Services",
                        "Other"
                );

            default:
                return Collections.singletonList("Other");
        }
    }


    private Account findAccountByName(String name) {
        for (Account a : accounts) {
            if (a.name.equals(name)) return a;
        }
        return null;
    }

    private boolean accountNameExists(String name) {
        if (name == null) return false;

        String normalized = name.trim().toLowerCase(Locale.ENGLISH);

        for (Account a : accounts) {
            if (a.name.trim().toLowerCase(Locale.ENGLISH).equals(normalized)) {
                return true;
            }
        }
        return false;
    }


    private String getMonthAbbrev(int i) {
        String[] m = {"Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        return m[i];
    }

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

                if (MainScreen.instance != null) {
                    MainScreen.instance.refreshAfterAdd();
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

    private void showManageCategoriesDialog() {

        View dv = requireActivity().getLayoutInflater()
                .inflate(R.layout.dialog_manage_categories, null);

        TextView btnAdd = dv.findViewById(R.id.btn_add);
        TextView btnEdit = dv.findViewById(R.id.btn_edit);
        TextView btnDelete = dv.findViewById(R.id.btn_delete);
        TextView btnReset = dv.findViewById(R.id.btn_reset);
        TextView btnCancel = dv.findViewById(R.id.btn_cancel);

        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle("Manage Categories")
                .setView(dv)
                .create();

        btnAdd.setOnClickListener(v -> {
            dialog.dismiss();
            showAddCategoryDialog();
        });

        btnEdit.setOnClickListener(v -> {
            dialog.dismiss();
            showEditCategoryDialog();
        });

        btnDelete.setOnClickListener(v -> {
            dialog.dismiss();
            showDeleteCategoryDialog();
        });

        btnReset.setOnClickListener(v -> {

            new AlertDialog.Builder(requireContext())
                    .setTitle("Reset Defaults")
                    .setMessage("Are you sure you want to reset all categories to default? This will remove your custom categories.")
                    .setPositiveButton("Yes, Reset", (d, w) -> {
                        CategoryManager.resetToDefault(requireContext());
                        setupSpinner();
                        Toast.makeText(requireContext(),
                                "Defaults restored", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void showAddCategoryDialog() {

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

                    CategoryManager.saveCategoryWithTag(requireContext(), name, tag);

                    lastSelectedCategory = name;

                    suppressSpinnerCallback = true;
                    loadCategoriesIntoSpinner(name);
                    suppressSpinnerCallback = false;

                    Toast.makeText(requireContext(),
                            "Added \"" + name + "\"", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showEditCategoryDialog() {

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

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Edit Tag");

        builder.setItems(items.toArray(new String[0]), (d, w) ->
                showTagChangeDialog(items.get(w)));

        builder.setNegativeButton("Cancel", null);

        AlertDialog dlg = builder.create();
        dlg.show();
    }

    private void showTagChangeDialog(String cat) {

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

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Change Tag");

        builder.setPositiveButton("Save", (dialog, w) -> {

            int sel = rg.getCheckedRadioButtonId();
            RadioButton rb = dv.findViewById(sel);
            String newTag = rb.getText().toString();

            CategoryManager.saveCategoryWithTag(requireContext(), cat, newTag);

            lastSelectedCategory = cat;
            setupSpinner();

            Toast.makeText(requireContext(),
                    "Updated tag", Toast.LENGTH_SHORT).show();
        });

        builder.setNegativeButton("Cancel", null);

        builder.setView(dv);

        AlertDialog dlg = builder.create();
        dlg.show();

        dlg.getButton(DialogInterface.BUTTON_NEGATIVE).setAllCaps(false);
        dlg.getButton(DialogInterface.BUTTON_POSITIVE).setAllCaps(false);
    }

    private void showDeleteCategoryDialog() {

        List<String> deletable = new ArrayList<>();
        for (String c : CategoryManager.getOrderedCategories(requireContext())) {
            if (!CategoryManager.isDefaultCategory(c)) {
                deletable.add(c);
            }
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
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void insertAccountExpense() {

        android.util.Log.e("ACCOUNTS", "STEP 1: entered insertAccountExpense");

        if (spinnerAccount == null) {
            android.util.Log.e("ACCOUNTS", "ABORT: spinnerAccount == null");
            return;
        }

        Object selected = spinnerAccount.getSelectedItem();
        android.util.Log.e("ACCOUNTS", "STEP 2: spinner selected = " + selected);

        if (selected == null) {
            android.util.Log.e("ACCOUNTS", "ABORT: selected == null");
            return;
        }

        String accountName = selected.toString();
        android.util.Log.e("ACCOUNTS", "STEP 3: accountName = [" + accountName + "]");

        ExpenseDatabase db =
                ExpenseDatabase.getDatabase(requireContext());

        AccountEntity account =
                db.accountDao().getAccountByName(accountName);

        android.util.Log.e(
                "ACCOUNTS",
                "STEP 4: account lookup result = " +
                        (account == null ? "NULL" : "id=" + account.id)
        );

        if (account == null) {
            android.util.Log.e("ACCOUNTS", "ABORT: account == null");
            return;
        }

        String item =
                editAccountItem.getText().toString().trim();

        android.util.Log.e("ACCOUNTS", "STEP 5: item = [" + item + "]");

        if (item.isEmpty()) {
            android.util.Log.e("ACCOUNTS", "ABORT: item empty");
            return;
        }

        String amountStr =
                editAccountAmount.getText().toString().trim();

        android.util.Log.e("ACCOUNTS", "STEP 6: amountStr = [" + amountStr + "]");

        if (amountStr.isEmpty()) {
            android.util.Log.e("ACCOUNTS", "ABORT: amount empty");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (Exception e) {
            android.util.Log.e("ACCOUNTS", "ABORT: amount parse failed");
            return;
        }

        String category =
                spinnerAccountCategory != null
                        ? spinnerAccountCategory.getSelectedItem().toString()
                        : "";
        android.util.Log.e("ACCOUNTS", "STEP 7: category = [" + category + "]");

        String note =
                editAccountNote != null
                        ? editAccountNote.getText().toString().trim()
                        : "";

        // ------------------------------------
        // AUTHORITATIVE DATE SYNC (UI → MODEL)
        // ------------------------------------
        try {
            TextView tv = getDialog().findViewById(R.id.text_account_date);
            if (tv != null) {
                Date d = new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.ENGLISH
                ).parse(tv.getText().toString());
                accountDate.setTime(d);
            }
        } catch (Exception e) {
            android.util.Log.e("ACCOUNTS", "DATE PARSE FAILED", e);
            return;
        }

        Calendar c = (Calendar) accountDate.clone();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        long dateMillis = c.getTimeInMillis();

        String date =
                new SimpleDateFormat(
                        "dd MMM yyyy",
                        Locale.ENGLISH
                ).format(c.getTime());

        android.util.Log.e("ACCOUNTS", "STEP 8: inserting account item");

        AccountItemEntity entity =
                new AccountItemEntity(
                        account.id,
                        date,
                        dateMillis,
                        item,
                        category,
                        amount,
                        note
                );

        db.accountItemDao().insert(entity);

        android.util.Log.e("ACCOUNTS", "STEP 9: insert complete");
    }

    private void applyActiveState(View daily, View accounts) {

        if (activeModule == ActiveModule.DAILY) {

            daily.setAlpha(1.0f);
            accounts.setAlpha(0.75f);

            setEnabledRecursive(daily, true);
            setEnabledRecursive(accounts, false);

        } else {

            daily.setAlpha(0.75f);
            accounts.setAlpha(1.0f);

            setEnabledRecursive(daily, false);
            setEnabledRecursive(accounts, true);
        }
    }

    private void setEnabledRecursive(View v, boolean enabled) {

        if (v instanceof ViewGroup) {
            ViewGroup vg = (ViewGroup) v;
            for (int i = 0; i < vg.getChildCount(); i++) {
                setEnabledRecursive(vg.getChildAt(i), enabled);
            }
        } else {
            v.setEnabled(enabled);
        }
    }

    private void saveActiveModule() {

        SharedPreferences prefs =
                requireContext().getSharedPreferences(
                        PREFS_UI,
                        Context.MODE_PRIVATE
                );

        prefs.edit()
                .putString(KEY_LAST_ACTIVE, activeModule.name())
                .apply();
    }



}
