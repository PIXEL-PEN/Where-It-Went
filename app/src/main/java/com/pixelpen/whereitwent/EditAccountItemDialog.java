package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class EditAccountItemDialog extends DialogFragment {

    public static final String ARG_ACCOUNT_ITEM_ID = "account_item_id";

    private Calendar editCalendar;
    private Spinner spinnerCategory;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        Dialog dialog = new Dialog(requireContext());

        View v = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_edit_account_item, null);

        dialog.setContentView(v);

        long itemId = -1L;
        if (getArguments() != null) {
            itemId = getArguments().getLong(ARG_ACCOUNT_ITEM_ID, -1L);
        }

        EditText editItem   = v.findViewById(R.id.edit_item);
        EditText editAmount = v.findViewById(R.id.edit_amount);
        EditText editNote   = v.findViewById(R.id.edit_note);
        TextView editDate   = v.findViewById(R.id.edit_date);

        spinnerCategory = v.findViewById(R.id.spinner_category);

        editCalendar = Calendar.getInstance();

        // --------------------
        // Load item
        // --------------------
        final AccountItemEntity[] loaded = new AccountItemEntity[1];

        ExpenseDatabase db = ExpenseDatabase.getDatabase(requireContext());
        AccountItemDao dao = db.accountItemDao();

        if (itemId > 0) {

            AccountItemEntity e = dao.getItemById(itemId);

            if (e != null) {

                loaded[0] = e;

                editItem.setText(e.item);
                editAmount.setText(String.valueOf(e.amount));
                editNote.setText(e.note != null ? e.note : "");
                editDate.setText(e.date);

                try {
                    SimpleDateFormat sdf =
                            new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);
                    editCalendar.setTime(sdf.parse(e.date));
                } catch (Exception ignored) {
                }
            }
        }

        // --------------------
        // Load categories (distinct from DB)
        // --------------------
        List<String> categories =
                dao.getDistinctCategoriesForAccount(loaded[0].accountId);


        // Safety fallback if empty
        if (categories == null) {
            categories = new ArrayList<>();
        }

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        requireContext(),
                        android.R.layout.simple_spinner_dropdown_item,
                        categories
                );

        spinnerCategory.setAdapter(adapter);

        // Preselect existing category
        if (loaded[0] != null && loaded[0].category != null) {
            int pos = categories.indexOf(loaded[0].category);
            if (pos >= 0) {
                spinnerCategory.setSelection(pos);
            }
        }

        // --------------------
        // Date picker
        // --------------------
        editDate.setOnClickListener(view1 -> {

            DatePickerDialog dlg =
                    new DatePickerDialog(
                            requireContext(),
                            (picker, y, m, d) -> {

                                editCalendar.set(y, m, d);

                                SimpleDateFormat sdf =
                                        new SimpleDateFormat(
                                                "dd MMM yyyy",
                                                Locale.ENGLISH
                                        );

                                editDate.setText(
                                        sdf.format(editCalendar.getTime())
                                );
                            },
                            editCalendar.get(Calendar.YEAR),
                            editCalendar.get(Calendar.MONTH),
                            editCalendar.get(Calendar.DAY_OF_MONTH)
                    );

            dlg.show();
        });

        TextView btnSave   = v.findViewById(R.id.btn_save);
        TextView btnDelete = v.findViewById(R.id.btn_delete);
        TextView btnCancel = v.findViewById(R.id.btn_cancel);

        // --------------------
        // SAVE
        // --------------------
        btnSave.setOnClickListener(vv -> {

            if (loaded[0] == null) return;

            loaded[0].item =
                    editItem.getText().toString().trim();

            loaded[0].note =
                    editNote.getText().toString().trim();

            try {
                loaded[0].amount = Double.parseDouble(
                        editAmount.getText().toString().trim()
                );
            } catch (Exception ex) {
                return;
            }

            loaded[0].category =
                    spinnerCategory.getSelectedItem().toString();

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH);

            loaded[0].date = sdf.format(editCalendar.getTime());
            loaded[0].dateMillis = editCalendar.getTimeInMillis();

            dao.update(loaded[0]);

            AccountsOverviewActivity.needsRefresh = true;

            dismiss();
        });

        // --------------------
        // DELETE
        // --------------------
        btnDelete.setOnClickListener(vv -> {

            if (loaded[0] == null) return;

            new androidx.appcompat.app.AlertDialog.Builder(requireContext())
                    .setTitle("Delete item?")
                    .setMessage("This cannot be undone.")
                    .setPositiveButton("Delete", (d, w) -> {
                        dao.delete(loaded[0]);
                        dismiss();
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });

        // --------------------
        // CANCEL
        // --------------------
        btnCancel.setOnClickListener(view1 -> dismiss());

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();
        Dialog dialog = getDialog();
        if (dialog != null && dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
            );
        }
    }
}
