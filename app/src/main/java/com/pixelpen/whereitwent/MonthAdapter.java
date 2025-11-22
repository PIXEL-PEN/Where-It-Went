package com.pixelpen.whereitwent;

import android.content.Context;
import android.content.Intent;
import android.view.*;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;


public class MonthAdapter extends RecyclerView.Adapter<MonthAdapter.Holder> {

    private final Context ctx;
    private final List<MonthGroup> data;
    private final LayoutInflater inflater;

    public MonthAdapter(Context c, List<MonthGroup> d) {
        ctx = c;
        data = d;
        inflater = LayoutInflater.from(c);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = inflater.inflate(R.layout.row_month_group, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder h, int pos) {

        MonthGroup mg = data.get(pos);

        h.textMonth.setText(mg.monthLabel);
        h.textTotal.setText(String.format(Locale.ENGLISH, "%.2f ₱", mg.total));

        h.containerEntries.removeAllViews();

        if (mg.expanded) {
            for (Expense e : mg.items) {

                View row = inflater.inflate(R.layout.row_month_entry, h.containerEntries, false);

                String ui = DateUtils.isoToUi(DateUtils.toIso(e.date));
                String stacked = DateUtils.toMonthStacked(ui);

                String[] parts = stacked.split(" ");
                h.setText(row, R.id.text_month_abbrev, parts[0]);
                h.setText(row, R.id.text_day_number, parts.length > 1 ? parts[1] : "--");

                h.setText(row, R.id.text_item, e.description);
                h.setText(row, R.id.text_category, e.category.toUpperCase());
                h.setText(row, R.id.text_amount, String.format(Locale.ENGLISH, "%.2f", e.amount));

                row.setOnClickListener(v -> {
                    Intent i = new Intent(ctx, DayDetailActivity.class);
                    i.putExtra("day", parts.length > 1 ? Integer.parseInt(parts[1]) : 1);
                    i.putExtra("month", parts[0]);
                    i.putExtra("year", mg.monthLabel.substring(mg.monthLabel.length() - 4));
                    ctx.startActivity(i);
                });

                h.containerEntries.addView(row);

                View div = new View(ctx);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, 1);
                div.setLayoutParams(lp);
                div.setBackgroundColor(0xFF555555);
                h.containerEntries.addView(div);
            }
        }
        h.containerEntries.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);

        h.header.setOnClickListener(v -> {
            mg.expanded = !mg.expanded;
            notifyItemChanged(pos);
        });
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView textMonth, textTotal;
        LinearLayout containerEntries;
        LinearLayout header;

        Holder(View v) {
            super(v);
            header = v.findViewById(R.id.group_header);
            textMonth = v.findViewById(R.id.text_group_month);
            textTotal = v.findViewById(R.id.text_group_total);
            containerEntries = v.findViewById(R.id.entries_container);
        }

        void setText(View row, int id, String value) {
            ((TextView) row.findViewById(id)).setText(value);
        }
    }
}
