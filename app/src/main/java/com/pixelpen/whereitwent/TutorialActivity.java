package com.pixelpen.whereitwent;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TutorialActivity extends AppCompatActivity {

    private TextView tvReadme;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        tvReadme = findViewById(R.id.tv_readme);

        // Load guide.md from /res/raw
        tvReadme.setText(loadGuideText());

        Button btnClose = findViewById(R.id.btn_close_readme);
        Button btnEdit  = findViewById(R.id.btn_edit_readme);

        btnClose.setOnClickListener(v -> {
            finish();
            overridePendingTransition(0,0);
        });

        btnEdit.setOnClickListener(v -> showEditDialog());
    }

    private String loadGuideText() {
        try {
            InputStream is = getResources().openRawResource(R.raw.guide);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }

            br.close();
            is.close();

            return sb.toString();

        } catch (Exception e) {
            return "Guide unavailable.";
        }
    }

    private void showEditDialog() {
        // Developer-side edit only (not persistent!)
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setMinLines(12);
        input.setText(tvReadme.getText());

        new AlertDialog.Builder(this)
                .setTitle("Edit Guide (Developer Only)")
                .setView(input)
                .setPositiveButton("Apply", (dialog, which) -> {
                    tvReadme.setText(input.getText());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
