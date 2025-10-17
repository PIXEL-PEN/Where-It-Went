package com.pixelpen.whereitwent;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import com.pixelpen.whereitwent.Expense;

import android.widget.TextView;
import androidx.appcompat.widget.AppCompatButton;
import java.util.Date;
import java.util.concurrent.Executors;


public class MainActivityReorder extends AppCompatActivity {

    private Spinner spinnerCategory;
    private EditText inputItem, inputAmount, inputDate;
    private Button buttonOk;

    private ExpenseDatabase db;
    private ExpenseDao expenseDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_reorder);

        EditText editDescription = findViewById(R.id.edit_description);
        EditText editAmount = findViewById(R.id.edit_amount);
        Spinner spinnerCategory = findViewById(R.id.spinner_category);
        TextView textDate = findViewById(R.id.text_date);
        AppCompatButton btnSave = findViewById(R.id.btn_save);

        // Spinner setup
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(
                this,
                R.array.category_names,
                android.R.layout.simple_spinner_item
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);

        // Date setup
        SimpleDateFormat sdf = new SimpleDateFormat("EEE. MMM dd yyyy", Locale.getDefault());
        Calendar calendar = Calendar.getInstance();
        textDate.setText(sdf.format(calendar.getTime()));

        textDate.setOnClickListener(v -> {
            int y = calendar.get(Calendar.YEAR);
            int m = calendar.get(Calendar.MONTH);
            int d = calendar.get(Calendar.DAY_OF_MONTH);
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                calendar.set(year, month, dayOfMonth);
                textDate.setText(sdf.format(calendar.getTime()));
            }, y, m, d).show();
        });

        // Save button logic
        btnSave.setOnClickListener(v -> {
            String description = editDescription.getText().toString().trim();
            String amountStr = editAmount.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String date = textDate.getText().toString();

            if (description.isEmpty() || amountStr.isEmpty()) {
                Toast.makeText(this, "Enter description and amount", Toast.LENGTH_SHORT).show();
                return;
            }

            double amount = Double.parseDouble(amountStr);

            Expense expense = new Expense();
            expense.description = description;
            expense.amount = amount;
            expense.category = category;
            expense.date = date;

            Executors.newSingleThreadExecutor().execute(() -> {
                ExpenseDatabase.getDatabase(this).expenseDao().insert(expense);

                runOnUiThread(() -> Toast.makeText(this, "Expense saved", Toast.LENGTH_SHORT).show());
            });
        });
    }
}