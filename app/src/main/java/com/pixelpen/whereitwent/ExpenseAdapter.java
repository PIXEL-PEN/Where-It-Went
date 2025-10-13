package com.pixelpen.whereitwent;

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
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;
import android.graphics.Color;
import android.util.Log;

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
        Context context = holder.itemView.getContext();

        holder.textCategory.setText(expense.category);
        holder.textDate.setText(expense.date);

        // temporary visible background for testing
        holder.viewTagDot.setVisibility(View.VISIBLE);
        holder.viewTagDot.setBackgroundColor(Color.MAGENTA);
        Log.d("ExpenseAdapter", "✅ set magenta background for position " + position);

        // 🎨 Shape logic for tag indicators
        String tag = CategoryManager.getTagForCategory(context, expense.category);
        View dot = holder.viewTagDot;
        if (dot != null) {
            switch (tag) {
                case "Basic":
                    dot.setVisibility(View.VISIBLE);
                    dot.setBackground(ContextCompat.getDrawable(context, R.drawable.tag_basic_square));
                    break;
                case "Discretionary":
                    dot.setVisibility(View.VISIBLE);
                    dot.setBackground(ContextCompat.getDrawable(context, R.drawable.tag_discretionary_triangle));
                    break;
                default:
                    dot.setVisibility(View.INVISIBLE); // hide Fixed
                    break;
            }
        }

        // 💰 Load user’s selected currency
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        // Format amount with smaller currency symbol
        String formatted = String.format(Locale.ENGLISH, "%.2f %s", expense.amount, symbol);
        SpannableString display = new SpannableString(formatted);
        int start = formatted.length() - symbol.length();
        display.setSpan(new RelativeSizeSpan(0.85f), start, formatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.textAmount.setText(display);

        // ✅ Row click → details dialog
        holder.itemView.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("Expense Details")
                    .setMessage("Category: " + expense.category + "\n"
                            + "Tag: " + tag + "\n"
                            + "Date: " + expense.date + "\n"
                            + "Item: " + expense.description + "\n"
                            + "Amount: " + formatted)
                    .setPositiveButton("Edit", (dialog, which) -> {
                        Intent intent = new Intent(context, AddExpenseActivity.class);
                        intent.putExtra("expense_id", expense.id);
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
        View viewTagDot;

        ExpenseViewHolder(@NonNull View itemView) {
            super(itemView);
            textCategory = itemView.findViewById(R.id.text_category);
            textDate = itemView.findViewById(R.id.text_date);
            textAmount = itemView.findViewById(R.id.text_amount);
            viewTagDot = itemView.findViewById(R.id.view_tag_dot);
        }
    }
}
