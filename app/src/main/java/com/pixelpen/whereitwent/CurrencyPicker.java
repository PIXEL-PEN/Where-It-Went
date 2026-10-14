package com.pixelpen.whereitwent;

import android.annotation.SuppressLint;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.MotionEvent;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * Quick find for the currency lists (v3.3). A tap on a currency spinner opens
 * a list with a search box: "riel", "camb" or "KHR" finds the Cambodian Riel.
 * Picking one sets the spinner, so its own listener runs as before.
 * The spinner's items must be TripCurrency.ALL, in that order.
 */
public final class CurrencyPicker {

    private CurrencyPicker() { }

    @SuppressLint("ClickableViewAccessibility")
    public static void attach(Spinner spinner) {
        spinner.setOnTouchListener((v, e) -> {
            if (e.getAction() == MotionEvent.ACTION_UP && v.isEnabled()) open(spinner);
            return true;   // instead of the long built-in dropdown
        });
    }

    private static void open(Spinner spinner) {
        android.content.Context ctx = spinner.getContext();
        float d = ctx.getResources().getDisplayMetrics().density;

        LinearLayout box = new LinearLayout(ctx);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(Math.round(20 * d), Math.round(8 * d), Math.round(20 * d), 0);

        EditText search = new EditText(ctx);
        search.setHint("Search: country, currency or code");
        search.setSingleLine(true);
        search.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        box.addView(search);

        ListView list = new ListView(ctx);
        box.addView(list, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.round(360 * d)));

        final List<Integer> shown = new ArrayList<>();
        final List<String> labels = new ArrayList<>();
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(ctx,
                android.R.layout.simple_list_item_1, labels);
        list.setAdapter(adapter);

        Runnable filter = () -> {
            shown.clear();
            labels.clear();
            for (int i : TripCurrency.search(search.getText().toString())) {
                shown.add(i);
                labels.add(TripCurrency.ALL.get(i).label());
            }
            adapter.notifyDataSetChanged();
            int sel = shown.indexOf(spinner.getSelectedItemPosition());
            if (sel >= 0 && search.getText().length() == 0) list.setSelection(Math.max(0, sel - 2));
        };
        filter.run();

        AlertDialog dialog = new AlertDialog.Builder(ctx)
                .setView(box)
                .setNegativeButton("Cancel", null)
                .create();
        list.setOnItemClickListener((p, v, pos, id) -> {
            spinner.setSelection(shown.get(pos));
            dialog.dismiss();
        });
        search.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable s) { filter.run(); }
        });
        dialog.show();
    }
}
