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
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ExpenseAdapter extends RecyclerView.Adapter<ExpenseAdapter.ExpenseViewHolder> {

    private final List<Expense> expenses;

    public ExpenseAdapter(List<Expense> initial) {
        this.expenses = (initial == null) ? new ArrayList<>() : new ArrayList<>(initial);
        setHasStableIds(false);
    }

    /** Pagination hook from ViewAllActivity */
    public void updateData(List<Expense> newPage) {
        expenses.clear();
        if (newPage != null) expenses.addAll(newPage);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ExpenseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_expense, parent, false);
        return new ExpenseViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ExpenseViewHolder holder, int position) {
        Expense e = expenses.get(position);
        Context ctx = holder.itemView.getContext();

        holder.textCategory.setText(e.category);
        holder.textDate.setText(e.date);

        SharedPreferences prefs = ctx.getSharedPreferences("settings", Context.MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        String formatted = String.format(Locale.ENGLISH, "%.2f %s", e.amount, symbol);
        SpannableString display = new SpannableString(formatted);
        int start = formatted.length() - symbol.length();
        display.setSpan(new RelativeSizeSpan(0.85f), start, formatted.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        holder.textAmount.setText(display);

        holder.itemView.setOnClickListener(v -> {
            String tag = CategoryManager.getTagForCategory(ctx, e.category);

            new AlertDialog.Builder(ctx)
                    .setTitle("Expense Details")
                    .setMessage("Category: " + e.category + " (" + tag + ")" + "\n"
                            + "Date: " + e.date + "\n"
                            + "Item: " + e.description + "\n"
                            + "Amount: " + formatted)
                    .setPositiveButton("Edit", (d, w) -> {
                        Intent intent = new Intent(ctx, AddExpenseActivity.class);
                        intent.putExtra("expense_id", e.id);
                        ctx.startActivity(intent);
                    })
                    .setNegativeButton("Delete", (d, w) -> {
                        ExpenseDatabase.getDatabase(ctx).expenseDao().delete(e);

                        int adapterPos = holder.getAdapterPosition();
                        if (adapterPos != RecyclerView.NO_POSITION) {
                            expenses.remove(adapterPos);
                            notifyItemRemoved(adapterPos);
                        } else {
                            expenses.remove(e);
                            notifyDataSetChanged();
                        }
                        Toast.makeText(ctx, "Expense deleted", Toast.LENGTH_SHORT).show();
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
