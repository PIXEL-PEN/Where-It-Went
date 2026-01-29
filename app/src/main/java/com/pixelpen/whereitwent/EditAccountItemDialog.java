package com.pixelpen.whereitwent;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class EditAccountItemDialog extends DialogFragment {

    public static final String ARG_ACCOUNT_ITEM_ID = "account_item_id";

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

        EditText editItem     = v.findViewById(R.id.edit_item);
        EditText editAmount   = v.findViewById(R.id.edit_amount);
        EditText editCategory = v.findViewById(R.id.edit_category);
        EditText editNote     = v.findViewById(R.id.edit_note);
        TextView editDate     = v.findViewById(R.id.edit_date);

        if (itemId > 0) {
            ExpenseDatabase db = ExpenseDatabase.getDatabase(requireContext());
            AccountItemDao dao = db.accountItemDao();

            AccountItemEntity e = dao.getItemById(itemId);
            if (e != null) {
                editItem.setText(e.item);
                editAmount.setText(String.valueOf(e.amount));
                editCategory.setText(e.category);
                editNote.setText(e.note != null ? e.note : "");
                editDate.setText(e.date);
            }
        }

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
