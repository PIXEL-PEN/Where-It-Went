package com.pixelpen.whereitwent;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MONTH  = 1;

    private final List<MainRow> rows;

    private static final int COLOR_HEADER_DEFAULT  = 0xFFFFFFFF;
    private static final int COLOR_HEADER_SELECTED = 0xFFECECEC;

    public MainAdapter(List<MainRow> rows) {
        this.rows = rows;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {
        MonthGroup mg = (MonthGroup) rows.get(position);
        return mg.isHeader ? TYPE_HEADER : TYPE_MONTH;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.row_month_group_header, parent, false);
            return new VH_Header(v);
        }

        View v = inflater.inflate(R.layout.row_month_group, parent, false);
        return new VH_Month(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        MonthGroup mg = (MonthGroup) rows.get(position);

        if (holder instanceof VH_Header) {
            VH_Header vh = (VH_Header) holder;

            vh.title.setText("Recent");

            vh.total.setText(
                    ((MainScreen) vh.itemView.getContext()).twelveMonthMode
                            ? "12 Months ▼"
                            : "3 Months ▼"
            );




            vh.total.setOnClickListener(v -> {

                MainScreen screen =
                        (MainScreen) vh.itemView.getContext();

                screen.twelveMonthMode =
                        !screen.twelveMonthMode;

                screen.refreshAfterAdd();
            });

            return;
        }


        VH_Month vh = (VH_Month) holder;

        vh.title.setText(mg.monthLabel);
        vh.total.setText(mg.totalFormatted);

        vh.header.setBackgroundColor(
                mg.expanded ? COLOR_HEADER_SELECTED : COLOR_HEADER_DEFAULT
        );

        vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);

        vh.header.setOnClickListener(v -> {

            boolean wasExpanded = mg.expanded;

            for (MainRow row : rows) {
                if (row instanceof MonthGroup) {
                    MonthGroup other = (MonthGroup) row;
                    if (!other.isHeader) {
                        other.expanded = false;
                    }
                }
            }

            mg.expanded = !wasExpanded;

            notifyDataSetChanged();
        });


        LayoutInflater inflater = LayoutInflater.from(vh.children.getContext());
        vh.children.removeAllViews();

        for (MonthGroup.DayData data : mg.dayRows) {

            View row = inflater.inflate(R.layout.row_month_entry, vh.children, false);

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

            row.setTag(data.iso);

            row.setOnClickListener(v -> {
                String iso = (String) v.getTag();
                if (iso != null) {
                    Intent intent = new Intent(v.getContext(), DayDetailActivity.class);
                    intent.putExtra("selected_date", iso);
                    v.getContext().startActivity(intent);
                }
            });

            if (mg.expanded) {
                row.setBackgroundColor(0xFFFFFEF9);
            } else {
                row.setBackgroundColor(0x00000000);
            }

            vh.children.addView(row);
        }
    }

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
