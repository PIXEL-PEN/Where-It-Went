package com.pixelpen.whereitwent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class AccountsOverviewAdapter
        extends RecyclerView.Adapter<AccountsOverviewAdapter.Holder> {

    @NonNull
    @Override
    public Holder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.row_account_section, parent, false);
        return new Holder(v);
    }

    @Override
    public void onBindViewHolder(
            @NonNull Holder holder,
            int position
    ) {
        holder.title.setText("Kitchen Renovation");
    }

    @Override
    public int getItemCount() {
        return 1; // single test row
    }

    static class Holder extends RecyclerView.ViewHolder {

        TextView title;

        Holder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.text_title);
        }
    }
}
