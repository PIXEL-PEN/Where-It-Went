package com.pixelpen.whereitwent;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class EditAccountItemDialog extends DialogFragment {

    public static final String ARG_ACCOUNT_ITEM_ID = "account_item_id";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {

        Dialog dialog = new Dialog(requireContext());
        dialog.setContentView(
                LayoutInflater.from(requireContext())
                        .inflate(R.layout.dialog_edit_account_item, null)
        );

        return dialog;
    }
}
