package com.pixelpen.whereitwent;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class AddExpenseDialog extends DialogFragment {

    private Spinner spinnerCategory;
    private EditText inputItem, inputAmount;
    private TextView textTag, textDate;
    private Button btnOk;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        View view = LayoutInflater.from(getActivity())
                .inflate(R.layout.dialog_add_expense, null);

        spinnerCategory = view.findViewById(R.id.spinner_category);
        inputItem = view.findViewById(R.id.input_item);
        inputAmount = view.findViewById(R.id.input_amount);
        textTag = view.findViewById(R.id.text_tag);
        textDate = view.findViewById(R.id.text_date);
        btnOk = view.findViewById(R.id.btn_ok);

        setupCategorySpinner();
        setupDate();
        setupButton();

        return new AlertDialog.Builder(requireContext())
                .setView(view)
                .setCancelable(true)
                .create();
    }

    private void setupCategorySpinner() {
        // TODO: Copy your old AddExpenseActivity spinner setup code here
    }

    private void setupDate() {
        // TODO: Copy your old auto-date code here
    }

    private void setupButton() {
        btnOk.setOnClickListener(v -> {
            // TODO: Insert into DB — copy your AddExpenseActivity logic

            dismiss();
        });
    }
}
