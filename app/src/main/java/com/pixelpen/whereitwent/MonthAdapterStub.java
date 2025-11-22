package com.pixelpen.whereitwent;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class MonthAdapterStub extends RecyclerView.Adapter<MonthAdapterStub.Holder> {

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Temporary row — we will replace later with real month row.
        TextView tv = new TextView(parent.getContext());
        tv.setPadding(40, 40, 40, 40);
        tv.setText("Month rows will appear here");
        return new Holder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        // empty, nothing to bind yet
    }

    @Override
    public int getItemCount() {
        // Option A = no months yet
        return 1;
    }

    static class Holder extends RecyclerView.ViewHolder {
        public Holder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
