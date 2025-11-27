package com.pixelpen.whereitwent;

import android.content.Intent;
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

    // Colors
    private static final int COLOR_HEADER_DEFAULT  = 0xFFFFFFFF; // white
    private static final int COLOR_HEADER_SELECTED = 0xFFECECEC; // pale grey

    public MonthAdapter(List<MonthGroup> groups) {
        this.groups = groups;
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
        // HEADER ROW (Last 12 Months — static)
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

        // Highlight expanded month header
        vh.header.setBackgroundColor(
                mg.expanded ? COLOR_HEADER_SELECTED : COLOR_HEADER_DEFAULT
        );

        // Show/hide children
        vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);

        // Click → toggle + collapse others
        vh.header.setOnClickListener(v -> {

            // Collapse all except clicked
            for (int i = 0; i < groups.size(); i++) {
                if (!groups.get(i).isHeader && i != position) {
                    groups.get(i).expanded = false;
                }
            }

            // Toggle clicked
            mg.expanded = !mg.expanded;

            notifyDataSetChanged();
        });

        // --------------------------------------------------
        // CHILD DAY ROWS
        // --------------------------------------------------
        LayoutInflater inflater = LayoutInflater.from(vh.children.getContext());
        vh.children.removeAllViews();

        for (int i = 0; i < mg.dayRows.size(); i++) {

            MonthGroup.DayData data = mg.dayRows.get(i);

            View row = inflater.inflate(R.layout.row_month_entry, vh.children, false);

            // Bind fields
            TextView m = row.findViewById(R.id.text_month_abbrev);
            TextView d = row.findViewById(R.id.text_day_number);
            TextView item = row.findViewById(R.id.text_item);
            TextView cat = row.findViewById(R.id.text_category);
            TextView amt = row.findViewById(R.id.text_amount);

            m.setText(data.monthAbbrev);
            d.setText(data.dayNumber);
            item.setText(data.description);
            cat.setText(data.category);
            amt.setText(data.amount);

            // Tag used to launch DayDetail
            row.setTag(data.iso);

            // Click → open DayDetailActivity
            row.setOnClickListener(v -> {
                String iso = (String) v.getTag();
                if (iso != null) {
                    Intent intent = new Intent(v.getContext(), DayDetailActivity.class);
                    intent.putExtra("selected_date", iso);
                    v.getContext().startActivity(intent);
                }
            });

            // Highlight all child rows when expanded
            if (mg.expanded) {
                row.setBackgroundColor(0xFFFFFAF2);   // pale amber
            } else {
                row.setBackgroundColor(0x00000000);   // transparent
            }

            vh.children.addView(row);
        }
    }

    // ------------------------------------------------------
    // VIEW HOLDERS
    // ------------------------------------------------------

    static class VH_Header extends RecyclerView.ViewHolder {
        TextView title, total;

        VH_Header(View v) {
            super(v);
            title = v.findViewById(R.id.text_group_header);
            total = v.findViewById(R.id.text_group_total);
        }
    }

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
