package com.pixelpen.whereitwent;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import java.util.ArrayList;
import java.util.List;

public class AccountCategoryFilterDialog extends DialogFragment {

    private Spinner spinnerAccount;
    private Spinner spinnerCategory;

    private ArrayAdapter<String> accountAdapter;
    private ArrayAdapter<String> categoryAdapter;

    private final List<AccountEntity> accounts = new ArrayList<>();

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_account_category_filter, null);

        spinnerAccount = v.findViewById(R.id.spinner_account);
        spinnerCategory = v.findViewById(R.id.spinner_category);

        // ----------------------------------------------------
        // ADAPTERS
        // ----------------------------------------------------
        accountAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        accountAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        categoryAdapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                new ArrayList<>()
        );
        categoryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerAccount.setAdapter(accountAdapter);
        spinnerCategory.setAdapter(categoryAdapter);

        // ----------------------------------------------------
        // LOAD ACCOUNTS
        // ----------------------------------------------------
        AccountDao accountDao =
                ExpenseDatabase.getDatabase(requireContext())
                        .accountDao();

        accounts.clear();
        accounts.addAll(accountDao.getActiveAccounts());

        List<String> accountNames = new ArrayList<>();
        accountNames.add("All Accounts");

        for (AccountEntity a : accounts) {
            accountNames.add(a.name);
        }

        accountAdapter.clear();
        accountAdapter.addAll(accountNames);
        accountAdapter.notifyDataSetChanged();

        spinnerAccount.setSelection(0);

        // ----------------------------------------------------
        // ACCOUNT → CATEGORY WIRING
        // ----------------------------------------------------
        spinnerAccount.setOnItemSelectedListener(
                new android.widget.AdapterView.OnItemSelectedListener() {

                    @Override
                    public void onItemSelected(
                            android.widget.AdapterView<?> parent,
                            View view,
                            int position,
                            long id
                    ) {

                        // All Accounts selected
                        if (position == 0) {
                            categoryAdapter.clear();
                            categoryAdapter.add("All Categories");
                            categoryAdapter.notifyDataSetChanged();

                            spinnerCategory.setSelection(0);
                            spinnerCategory.setEnabled(false);
                            return;
                        }

                        // Specific account selected
                        spinnerCategory.setEnabled(true);

                        AccountEntity account =
                                accounts.get(position - 1);

                        AccountItemDao itemDao =
                                ExpenseDatabase.getDatabase(requireContext())
                                        .accountItemDao();

                        List<String> categories =
                                itemDao.getCategoriesForAccount(account.id);

                        List<String> display = new ArrayList<>();
                        display.add("All Items");
                        display.addAll(categories);

                        categoryAdapter.clear();
                        categoryAdapter.addAll(display);
                        categoryAdapter.notifyDataSetChanged();

                        spinnerCategory.setSelection(0);
                    }

                    @Override
                    public void onNothingSelected(
                            android.widget.AdapterView<?> parent
                    ) {
                        // no-op
                    }
                }
        );

        return new AlertDialog.Builder(requireContext())
                .setTitle("Filter Accounts")
                .setView(v)
                .setNegativeButton("Clear", (d, w) -> dismiss())
                .setPositiveButton("Apply", (d, w) -> dismiss())
                .create();
    }
}
