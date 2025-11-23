package com.pixelpen.whereitwent;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

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

        // ------------------------------------------
        // HEADER
        // ------------------------------------------
        if (h instanceof VH_Header) {
            ((VH_Header) h).label.setText("Last 12 Months");
            return;
        }

        // ------------------------------------------
        // MONTH ROW
        // ------------------------------------------
        VH_Month vh = (VH_Month) h;

        vh.title.setText(mg.monthLabel);

        // Bind total (restored)
        vh.total.setText(mg.total);

        // Children visibility
        vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);

        // Toggle expand/collapse
        vh.header.setOnClickListener(v -> {
            mg.expanded = !mg.expanded;
            vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);
        });

        // ------------------------------------------
        // SAFE CHILD VIEW HANDLING
        // ------------------------------------------
        vh.children.removeAllViews();

        for (View child : mg.dayRows) {

            // Fix crash: detach from old parent if exists
            if (child.getParent() != null) {
                ((ViewGroup) child.getParent()).removeView(child);
            }

            vh.children.addView(child);
        }
    }

    // ------------------------------------------
    // VIEW HOLDERS
    // ------------------------------------------

    static class VH_Header extends RecyclerView.ViewHolder {
        TextView label;
        VH_Header(View v) {
            super(v);
            label = v.findViewById(R.id.text_group_header);
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
