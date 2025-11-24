package com.pixelpen.whereitwent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MonthAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MONTH  = 1;

    private final List<MonthGroup> groups;
    private final String currencySymbol;

    public MonthAdapter(List<MonthGroup> groups, String currencySymbol) {
        this.groups = groups;
        this.currencySymbol = currencySymbol;
    }

    @Override
    public int getItemViewType(int position) {
        return groups.get(position).isHeader ? TYPE_HEADER : TYPE_MONTH;
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (type == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.row_month_group_header, parent, false);
            return new VH_Header(v);
        }

        View v = inflater.inflate(R.layout.row_month_group, parent, false);
        return new VH_Month(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int position) {
        MonthGroup mg = groups.get(position);

        // --------------------------------------------------
        // HEADER ROW
        // --------------------------------------------------
        if (h instanceof VH_Header) {
            VH_Header vh = (VH_Header) h;
            vh.title.setText("Last 12 Months");
            vh.total.setText(mg.totalFormatted);
            return;
        }

        // --------------------------------------------------
        // MONTH ROW
        // --------------------------------------------------
        VH_Month vh = (VH_Month) h;

        vh.title.setText(mg.monthLabel);
        vh.total.setText(mg.totalFormatted);

        // Expand/collapse
        vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);

        vh.header.setOnClickListener(v -> {
            mg.expanded = !mg.expanded;
            notifyItemChanged(position);
        });

        // Remove old child views BEFORE adding new
        vh.children.removeAllViews();

        // Add day rows
        for (int i = 0; i < mg.dayRows.size(); i++) {
            View child = mg.dayRows.get(i);

            // Highlight newest entry only (top row)
            if (i == 0) {
                child.setBackgroundColor(0xFFFFF4D0); // light amber
            } else {
                child.setBackgroundColor(0x00000000); // clear
            }

            vh.children.addView(child);
        }
    }

    // =====================================================
    // HEADER HOLDER
    // =====================================================
    static class VH_Header extends RecyclerView.ViewHolder {
        TextView title, total;

        VH_Header(View v) {
            super(v);
            title = v.findViewById(R.id.text_group_header);
            total = v.findViewById(R.id.text_group_total);
        }
    }

    // =====================================================
    // MONTH HOLDER
    // =====================================================
    static class VH_Month extends RecyclerView.ViewHolder {
        View header;
        TextView title;
        TextView total;
        ViewGroup children;

        VH_Month(View v) {
            super(v);
            header   = v.findViewById(R.id.layout_month_header);
            title    = v.findViewById(R.id.text_month_title);
            total    = v.findViewById(R.id.text_month_total);
            children = v.findViewById(R.id.layout_month_children);
        }
    }
}
