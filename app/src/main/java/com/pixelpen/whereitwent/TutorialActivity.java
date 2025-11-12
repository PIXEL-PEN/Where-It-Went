package com.pixelpen.whereitwent;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

public class TutorialActivity extends AppCompatActivity {

    private static final String PREFS = "settings";
    private static final String KEY_README = "guide_readme";
    private static final String KEY_ENABLE_EDIT = "enable_guide_edit";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        TextView readme = findViewById(R.id.tv_readme);
        Button edit = findViewById(R.id.btn_edit_readme);

        // Load current README text (or a simple default)
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        String text = prefs.getString(KEY_README,
                "Welcome to Where It Went.\n\n" +
                        "This is a living guide. Tap EDIT to update these notes during development.");
        if (readme != null) readme.setText(text);

        // Optional: toggle edit button visibility via a flag in SharedPreferences
        boolean canEdit = prefs.getBoolean(KEY_ENABLE_EDIT, true);
        if (edit != null) edit.setVisibility(canEdit ? View.VISIBLE : View.GONE);

        if (edit != null) {
            edit.setOnClickListener(v -> showEditDialog(readme));
            Button close = findViewById(R.id.btn_close_readme);
            if (close != null) close.setOnClickListener(v -> finish());



        }
    }

    private void showEditDialog(TextView target) {
        if (target == null) return;

        final EditText input = new EditText(this);
        input.setMinLines(6);
        input.setText(target.getText());

        new AlertDialog.Builder(this)
                .setTitle("Edit Guide")
                .setView(input)
                .setPositiveButton("Save", (DialogInterface dialog, int which) -> {
                    String newText = input.getText().toString();
                    target.setText(newText);
                    getSharedPreferences(PREFS, MODE_PRIVATE)
                            .edit()
                            .putString(KEY_README, newText)
                            .apply();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
