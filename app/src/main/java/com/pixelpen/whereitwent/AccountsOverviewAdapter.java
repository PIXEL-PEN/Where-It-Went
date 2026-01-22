
package com.pixelpen.whereitwent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;





public class AccountsOverviewAdapter
        extends RecyclerView.Adapter<AccountsOverviewAdapter.SectionVH> {

    private final List<String> sections = new ArrayList<>();

    public AccountsOverviewAdapter() {
        sections.add("PROJECTS");
        sections.add("TRAVEL");
        sections.add("CUSTOM");
    }

    @Override
    public SectionVH onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_account_section, parent, false);
        return new SectionVH(v);
    }

    @Override
    public void onBindViewHolder(SectionVH holder, int position) {
        holder.text.setText(sections.get(position));
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class SectionVH extends RecyclerView.ViewHolder {
        TextView text;

        SectionVH(View itemView) {
            super(itemView);
            text = itemView.findViewById(R.id.text_title);
        }
    }
}
