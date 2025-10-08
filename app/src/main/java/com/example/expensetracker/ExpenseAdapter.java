package com.example.expensetracker;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenses;

    public ExpenseAdapter(List<Expense> expenses) {
        this.expenses = expenses;
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense expense = expenses.get(position);
        holder.textCategory.setText(expense.category);
        holder.textDate.setText(expense.date);

        // Load user’s selected currency
        SharedPreferences prefs = holder.itemView.getContext()
                .getSharedPreferences("settings", Context.MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        // Format amount with smaller currency symbol after number
        String formatted = String.format(Locale.ENGLISH, "%.2f %s", expense.amount, symbol);
        SpannableString display = new SpannableString(formatted);
        int start = formatted.length() - symbol.length();
        display.setSpan(new RelativeSizeSpan(0.85f), start, formatted.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.textAmount.setText(display);

        // ✅ Row click → show details dialog
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();

            new AlertDialog.Builder(context)
                    .setTitle("Expense Details")
                    .setMessage("Category: " + expense.category + "\n"
                            + "Date: " + expense.date + "\n"
                            + "Item: " + expense.description + "\n"
                            + "Amount: " + formatted)
                    .setPositiveButton("Edit", (dialog, which) -> {
                        Intent intent = new Intent(context, AddExpenseActivity.class);
                        intent.putExtra("expense_id", expense.id);  // pass ID for editing
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Delete", (dialog, which) -> {
                        ExpenseDatabase.getDatabase(context).expenseDao().delete(expense);
                        expenses.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("Close", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    static class ExpenseViewHolder extends RecyclerView.ViewHolder {
        TextView textCategory, textDate, textAmount;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.text_category);
            textDate = itemView.findViewById(R.id.text_date);
            textAmount = itemView.findViewById(R.id.text_amount);
        }
    }
}
