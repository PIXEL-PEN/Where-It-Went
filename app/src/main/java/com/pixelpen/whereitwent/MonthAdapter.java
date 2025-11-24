package com.pixelpen.whereitwent;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class MonthAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_GROUP_HEADER = 0;
    private static final int TYPE_MONTH = 1;

    private final List<MonthGroup> groups;
    private final LayoutInflater inflater;

    public MonthAdapter(Context ctx, List<MonthGroup> list) {
        this.groups = list;
        this.inflater = LayoutInflater.from(ctx);
    }

    @Override
    public int getItemViewType(int position) {
        return groups.get(position).isHeader ? TYPE_GROUP_HEADER : TYPE_MONTH;
    }

    @Override
    public int getItemCount() {
        return groups.size();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int type) {

        if (type == TYPE_GROUP_HEADER) {
            View v = inflater.inflate(R.layout.row_month_group_header, parent, false);
            return new VH_Header(v);
        }

        View v = inflater.inflate(R.layout.row_month_group, parent, false);
        return new VH_Month(v);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder h, int pos) {

        MonthGroup mg = groups.get(pos);

        // --------------------------------------------------
        // HEADER ROW ("Last 12 Months")
        // --------------------------------------------------
        if (h instanceof VH_Header) {
            VH_Header vh = (VH_Header) h;
            vh.label.setText(mg.monthLabel);
            vh.total.setText(mg.total);   // <-- FIXED: set header total
            return;
        }

        // --------------------------------------------------
        // MONTH ROW
        // --------------------------------------------------
        VH_Month vh = (VH_Month) h;

        vh.title.setText(mg.monthLabel);
        vh.total.setText(mg.total);  // month total

        vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);

        // Safe expand/collapse without triggering rebind
        vh.header.setOnClickListener(v -> {
            mg.expanded = !mg.expanded;
            vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);
        });

        // --------------------------------------------------
        // SAFELY ATTACH CHILD DAY-VIEWS
        // --------------------------------------------------
        vh.children.removeAllViews();

        for (View child : mg.dayRows) {

            // Prevent "View already has a parent" crash
            if (child.getParent() != null) {
                ((ViewGroup) child.getParent()).removeView(child);
            }

            vh.children.addView(child);
        }
    }

    // --------------------------------------------------
    // VIEW HOLDERS
    // --------------------------------------------------

    static class VH_Header extends RecyclerView.ViewHolder {
        TextView label;
        TextView total;

        VH_Header(View v) {
            super(v);
            label = v.findViewById(R.id.text_group_header);
            total = v.findViewById(R.id.text_group_total);   // <-- binds header total
        }
    }

    static class VH_Month extends RecyclerView.ViewHolder {
        TextView title;
        TextView total;
        ViewGroup children;
        View header;

        VH_Month(View v) {
            super(v);
            header = v.findViewById(R.id.layout_month_header);
            title = v.findViewById(R.id.text_month_title);
            total = v.findViewById(R.id.text_month_total);
            children = v.findViewById(R.id.layout_month_children);
        }
    }
}
