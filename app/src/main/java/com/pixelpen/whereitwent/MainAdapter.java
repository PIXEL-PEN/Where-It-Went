package com.pixelpen.whereitwent;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;



public class MainAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_MONTH  = 1;

    private final List<MainRow> rows;

    private static final int COLOR_HEADER_DEFAULT  = 0xFFFFFFFF;
    private static final int COLOR_HEADER_SELECTED = 0xFFECECEC;

    private static final int TYPE_SECTION_HEADER = 2;
    private static final int TYPE_SUMMARY = 3;



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
            return mg.isHeader ? TYPE_HEADER : TYPE_MONTH;
        }

        if (row instanceof RowSectionHeader) {
            return TYPE_SECTION_HEADER;
        }

        if (row instanceof RowSummary) {
            return TYPE_SUMMARY;
        }

        return TYPE_MONTH; // fallback
    }


    @Override
    @NonNull
    public RecyclerView.ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {

        LayoutInflater inflater = LayoutInflater.from(parent.getContext());

        if (viewType == TYPE_HEADER) {
            View v = inflater.inflate(R.layout.row_month_group_header, parent, false);
            return new VH_Header(v);
        }

        if (viewType == TYPE_MONTH) {
            View v = inflater.inflate(R.layout.row_month_group, parent, false);
            return new VH_Month(v);
        }

        if (viewType == TYPE_SECTION_HEADER) {
            View v = inflater.inflate(R.layout.row_section_header, parent, false);
            return new VH_SectionHeader(v);
        }

        if (viewType == TYPE_SUMMARY) {
            View v = inflater.inflate(R.layout.row_summary, parent, false);
            return new VH_Summary(v);
        }

        throw new IllegalStateException("Unknown viewType: " + viewType);
    }

    @Override
    public void onBindViewHolder(
            @NonNull RecyclerView.ViewHolder holder,
            int position
    ) {

        MainRow row = rows.get(position);

        // -----------------------------------
        // RECENT HEADER (MonthGroup header)
        // -----------------------------------
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

        // -----------------------------------
        // SUMMARIES SECTION HEADER
        // -----------------------------------
        if (holder instanceof VH_SectionHeader) {

            RowSectionHeader section = (RowSectionHeader) row;
            VH_SectionHeader vh = (VH_SectionHeader) holder;

            vh.title.setText(section.title);
            return;
        }

        // -----------------------------------
        // SUMMARY ROW
        // -----------------------------------
        if (holder instanceof VH_Summary) {

            RowSummary summary = (RowSummary) row;
            VH_Summary vh = (VH_Summary) holder;

            String full = summary.label;

            int start = full.indexOf("(");

            if (start != -1) {

                SpannableString span = new SpannableString(full);

                span.setSpan(
                        new RelativeSizeSpan(0.85f),
                        start,
                        full.length(),
                        Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );

                vh.label.setText(span);

            } else {
                vh.label.setText(full);
            }

            vh.value.setText(summary.value);
            return;
        }

        // -----------------------------------
        // MONTH ROW
        // -----------------------------------
        MonthGroup mg = (MonthGroup) row;
        VH_Month vh = (VH_Month) holder;

        vh.title.setText(mg.monthLabel);
        vh.total.setText(mg.totalFormatted);

        vh.header.setBackgroundColor(
                mg.expanded ? COLOR_HEADER_SELECTED : COLOR_HEADER_DEFAULT
        );

        vh.children.setVisibility(mg.expanded ? View.VISIBLE : View.GONE);

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

            View child =
                    inflater.inflate(R.layout.row_month_entry,
                            vh.children,
                            false);

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

            child.setTag(data.iso);

            child.setOnClickListener(v -> {
                String iso = (String) v.getTag();
                if (iso != null) {
                    Intent intent =
                            new Intent(v.getContext(),
                                    DayDetailActivity.class);
                    intent.putExtra("selected_date", iso);
                    v.getContext().startActivity(intent);
                }
            });

            child.setBackgroundColor(
                    mg.expanded ? 0xFFFFFEF9 : 0x00000000
            );

            vh.children.addView(child);
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

    static class VH_SectionHeader extends RecyclerView.ViewHolder {

        TextView title;

        VH_SectionHeader(View v) {
            super(v);
            title = v.findViewById(R.id.text_section_header);
        }
    }
    static class VH_Summary extends RecyclerView.ViewHolder {

        TextView label;
        TextView value;

        VH_Summary(View v) {
            super(v);
            label = v.findViewById(R.id.text_summary_label);
            value = v.findViewById(R.id.text_summary_value);
        }
    }


}
