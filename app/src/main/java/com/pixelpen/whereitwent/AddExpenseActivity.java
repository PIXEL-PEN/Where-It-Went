package com.pixelpen.whereitwent;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

public class AddExpenseActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent i = new Intent(this, MainActivity.class);
        if (getIntent() != null && getIntent().getExtras() != null) {
            i.putExtras(getIntent().getExtras()); // forward expense_id, etc.
        }
        startActivity(i);
        finish();
    }
}
