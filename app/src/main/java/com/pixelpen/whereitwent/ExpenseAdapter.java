package com.pixelpen.whereitwent;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
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
        Context context = holder.itemView.getContext();

        holder.textCategory.setText(expense.category);
        holder.textDate.setText(expense.date);

        // 🏷 Retrieve category tag
        String catTag = CategoryManager.getTagForCategory(context, expense.category);

        // 💰 Load user’s selected currency
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        // Format amount with smaller currency symbol
        String formatted = String.format(Locale.ENGLISH, "%.2f %s", expense.amount, symbol);
        SpannableString display = new SpannableString(formatted);
        int start = formatted.length() - symbol.length();
        display.setSpan(new RelativeSizeSpan(0.85f), start, formatted.length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.textAmount.setText(display);

        // ✅ Row click → details dialog
        holder.itemView.setOnClickListener(v -> {

            View dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_expense_details, null);
            TextView textDetails = dialogView.findViewById(R.id.text_details);
            TextView editCategoryLink = dialogView.findViewById(R.id.text_edit_category);

            String details = "Category: " + expense.category + " (" + catTag + ")\n"
                    + "Date: " + expense.date + "\n"
                    + "Item: " + expense.description + "\n"
                    + "Amount: " + String.format(Locale.ENGLISH, "%.2f", expense.amount);
            textDetails.setText(details);

            // 🔹 Inline bold [Edit Category Tag] link
            SpannableString label = new SpannableString("[Edit Category Tag]");
            label.setSpan(new StyleSpan(Typeface.BOLD), 0, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            editCategoryLink.setText(label);

            editCategoryLink.setOnClickListener(x -> {
                Intent intent = new Intent(context, AddExpenseActivity.class);
                intent.putExtra("open_category_editor", true);
                intent.putExtra("category_name", expense.category);

                if (context instanceof AppCompatActivity) {
                    AppCompatActivity activity = (AppCompatActivity) context;
                    activity.startActivityForResult(intent, 9001);
                } else {
                    context.startActivity(intent);
                }
            });

            new AlertDialog.Builder(context)
                    .setTitle("Expense Details")
                    .setView(dialogView)
                    .setNegativeButton("Delete", (dialog, which) -> {
                        ExpenseDatabase.getDatabase(context).expenseDao().delete(expense);
                        expenses.remove(position);
                        notifyItemRemoved(position);
                        Toast.makeText(context, "Expense deleted", Toast.LENGTH_SHORT).show();
                    })
                    .setNeutralButton("Close", null)
                    .setPositiveButton("Edit", (dialog, which) -> {
                        Intent intent = new Intent(context, AddExpenseActivity.class);
                        intent.putExtra("expense_id", expense.id);
                        context.startActivity(intent);
                    })
                    .show();
        });
    }

    // 🔁 Called when returning from AddExpenseActivity to refresh tags
    public void handleActivityResult(Context context, int requestCode, int resultCode) {
        if (requestCode == 9001 && resultCode == AppCompatActivity.RESULT_OK) {
            notifyDataSetChanged();
        }
    }

    @Override
    public int getItemCount() {
        return expenses.size();
    }

    // Optional helper for data reload
    public void updateData(List<Expense> newData) {
        expenses.clear();
        expenses.addAll(newData);
        notifyDataSetChanged();
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
