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

    private static final int TYPE_RECENT_HEADER = 0;
    private static final int TYPE_MONTH = 1;
    private static final int TYPE_SUMMARY_HEADER = 2;
    private static final int TYPE_SUMMARY = 3;

    private final List<MainRow> rows;

    public MainAdapter(List<MainRow> rows) {
        this.rows = rows;
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    @Override
    public int getItemViewType(int position) {

        MainRow row = rows.get(position);

        if (row instanceof MonthGroup) {
            MonthGroup mg = (MonthGroup) row;
            return mg.isHeader ? TYPE_RECENT_HEADER : TYPE_MONTH;
        }

        if (row instanceof RowSummaryHeader) {
            return TYPE_SUMMARY_HEADER;
        }

        if (row instanceof RowSummary) {
            return TYPE_SUMMARY;
        }

        throw new IllegalStateException("Unknown row type");
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_RECENT_HEADER) {
            View v = inflater.inflate(
                    R.layout.row_month_group_header,
                    parent,
                    false
            );
            return new VH_RecentHeader(v);
        }

        if (viewType == TYPE_MONTH) {
            View v = inflater.inflate(
                    R.layout.row_month_group,
                    parent,
                    false
            );
            return new VH_Month(v);
        }

        if (viewType == TYPE_SUMMARY_HEADER) {
            View v = inflater.inflate(
                    R.layout.row_month_group_header,
                    parent,
                    false
            );
            return new VH_SummaryHeader(v);
        }

        if (viewType == TYPE_SUMMARY) {
            View v = inflater.inflate(
                    R.layout.row_summary,
                    parent,
                    false
            );
            return new VH_Summary(v);
        }

        throw new IllegalStateException("Unknown viewType");
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        MainRow row = rows.get(position);

        // RECENT HEADER
        if (holder instanceof VH_RecentHeader) {

            VH_RecentHeader vh = (VH_RecentHeader) holder;
            MainScreen screen =
                    (MainScreen) vh.itemView.getContext();

            vh.title.setText("Recent");

            vh.total.setText(
                    screen.isTwelveMonthMode()
                            ? "12 Months ▼"
                            : "3 Months ▼"
            );

            vh.total.setOnClickListener(v -> {
                screen.toggleTwelveMonthMode();
                screen.refreshAfterAdd();
            });

            return;
        }

        // SUMMARY HEADER
        if (holder instanceof VH_SummaryHeader) {

            VH_SummaryHeader vh = (VH_SummaryHeader) holder;
            vh.title.setText("Summaries");
            vh.total.setText("▼");

            return;
        }

        // SUMMARY ROW
        if (holder instanceof VH_Summary) {

            RowSummary summary = (RowSummary) row;
            VH_Summary vh = (VH_Summary) holder;

            vh.label.setText(summary.label);
            vh.value.setText(summary.value);

            return;
        }

        // MONTH ROW
        if (holder instanceof VH_Month) {

            MonthGroup mg = (MonthGroup) row;
            VH_Month vh = (VH_Month) holder;

            vh.title.setText(mg.monthLabel);
            vh.total.setText(mg.totalFormatted);

            vh.children.setVisibility(
                    mg.expanded ? View.VISIBLE : View.GONE
            );

            vh.header.setOnClickListener(v -> {

                boolean wasExpanded = mg.expanded;

                for (MainRow r : rows) {
                    if (r instanceof MonthGroup) {
                        MonthGroup other = (MonthGroup) r;
                        if (!other.isHeader) {
                            other.expanded = false;
                        }
                    }
                }

                mg.expanded = !wasExpanded;

                notifyDataSetChanged();
            });

            LayoutInflater inflater =
                    LayoutInflater.from(vh.children.getContext());

            vh.children.removeAllViews();

            for (MonthGroup.DayData data : mg.dayRows) {

                View child = inflater.inflate(
                        R.layout.row_month_entry,
                        vh.children,
                        false
                );

                TextView m = child.findViewById(R.id.text_month_abbrev);
                TextView d = child.findViewById(R.id.text_day_number);
                TextView item = child.findViewById(R.id.text_item);
                TextView cat = child.findViewById(R.id.text_category);
                TextView amt = child.findViewById(R.id.text_amount);

                m.setText(data.monthAbbrev);
                d.setText(data.dayNumber);
                item.setText(data.description);
                cat.setText(data.category);
                amt.setText(data.amount);

                child.setOnClickListener(v -> {
                    Intent intent = new Intent(
                            v.getContext(),
                            DayDetailActivity.class
                    );
                    intent.putExtra("selected_date", data.iso);
                    v.getContext().startActivity(intent);
                });

                vh.children.addView(child);
            }

            return;
        }
    }

    // -----------------------
    // VIEW HOLDERS
    // -----------------------

    static class VH_RecentHeader extends RecyclerView.ViewHolder {

        TextView title, total;

        VH_RecentHeader(View v) {
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

    static class VH_SummaryHeader extends RecyclerView.ViewHolder {

        TextView title, total;

        VH_SummaryHeader(View v) {
            super(v);
            title = v.findViewById(R.id.text_group_header);
            total = v.findViewById(R.id.text_group_total);
        }
    }

    static class VH_Summary extends RecyclerView.ViewHolder {

        TextView label, value;

        VH_Summary(View v) {
            super(v);
            label = v.findViewById(R.id.text_summary_label);
            value = v.findViewById(R.id.text_summary_value);
        }
    }
}
