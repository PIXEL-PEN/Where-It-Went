package com.pixelpen.whereitwent;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TutorialActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial);

        // Load markdown text from raw resource
        TextView tv = findViewById(R.id.tv_readme);
        tv.setText(loadGuideMarkdown());

        // Back via CLOSE button
        Button btnClose = findViewById(R.id.btn_close_readme);
        if (btnClose != null) {
            btnClose.setOnClickListener(v -> finish());
        }
    }

    // Reads raw/guide.md → String
    private String loadGuideMarkdown() {
        try {
            InputStream is = getResources().openRawResource(R.raw.guide);
            BufferedReader br = new BufferedReader(new InputStreamReader(is));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
            br.close();
            return sb.toString();
        } catch (Exception e) {
            return "Unable to load guide.";
        }
    }
}
