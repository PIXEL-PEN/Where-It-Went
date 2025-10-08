package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
// import android.widget.LinearLayout;  // Legacy layout reference — no longer needed

import androidx.appcompat.app.AppCompatActivity;

/**
 * Legacy main screen preserved for reference.
 * This activity is no longer used as a launcher.
 * All clickable views are commented out to prevent missing-ID errors.
 */
public class MainLegacyActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // --- Legacy buttons from the old layout (no longer in use) ---
        // LinearLayout btnAdd = findViewById(R.id.btnAdd);
        // LinearLayout btnView = findViewById(R.id.btnView);
        // LinearLayout btnGear = findViewById(R.id.btnGear);

        // if (btnAdd != null) {
        //     btnAdd.setOnClickListener(v ->
        //             startActivity(new Intent(MainLegacyActivity.this, AddExpenseActivity.class))
        //     );
        // }

        // if (btnView != null) {
        //     btnView.setOnClickListener(v ->
        //             startActivity(new Intent(MainLegacyActivity.this, ViewMenuActivity.class))
        //     );
        // }

        // if (btnGear != null) {
        //     // Short press → open Settings
        //     btnGear.setOnClickListener(v ->
        //             startActivity(new Intent(MainLegacyActivity.this, SettingsActivity.class))
        //     );
        // }
    }
}
