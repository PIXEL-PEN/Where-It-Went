package com.pixelpen.whereitwent;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.RelativeSizeSpan;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ViewAllActivity extends AppCompatActivity {

    private static final int PAGE_SIZE = 15;

    private RecyclerView recyclerView;
    private ExpenseAdapter adapter;
    private TextView tvTotalAmount;
    private TextView btnPrev, btnNext, pageStatus;

    private final List<Expense> all = new ArrayList<>();
    private int pageIndex = 0; // 0-based page

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_all);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            });
        }

        recyclerView = findViewById(R.id.recycler_expenses);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.addItemDecoration(new DividerItemDecoration(this));

        tvTotalAmount = findViewById(R.id.tvTotalAmount);
        btnPrev = findViewById(R.id.btn_prev_page);
        btnNext = findViewById(R.id.btn_next_page);
        pageStatus = findViewById(R.id.page_status);

        List<Expense> loaded = ExpenseDatabase.getDatabase(this).expenseDao().getAll();
        all.clear();
        all.addAll(loaded);

        Collections.sort(all, (e1, e2) -> {
            Date d1 = parseDate(e1.date);
            Date d2 = parseDate(e2.date);
            if (d1 != null && d2 != null) {
                int cmp = d2.compareTo(d1);
                if (cmp != 0) return cmp;
            } else if (d1 == null ^ d2 == null) {
                return (d1 == null) ? 1 : -1;
            }
            return Integer.compare(e2.id, e1.id);
        });

        adapter = new ExpenseAdapter(sliceForPage(pageIndex));
        recyclerView.setAdapter(adapter);

        updatePager();
        updateTotalFooter(sliceForPage(pageIndex));

        if (btnPrev != null) {
            btnPrev.setOnClickListener(v -> {
                if (pageIndex > 0) {
                    pageIndex--;
                    adapter.updateData(sliceForPage(pageIndex));
                    updatePager();
                    updateTotalFooter(sliceForPage(pageIndex));
                    recyclerView.scrollToPosition(0);
                }
            });
        }
        if (btnNext != null) {
            btnNext.setOnClickListener(v -> {
                if ((pageIndex + 1) * PAGE_SIZE < all.size()) {
                    pageIndex++;
                    adapter.updateData(sliceForPage(pageIndex));
                    updatePager();
                    updateTotalFooter(sliceForPage(pageIndex));
                    recyclerView.scrollToPosition(0);
                }
            });
        }
    }

    private List<Expense> sliceForPage(int page) {
        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, all.size());
        if (start >= end || start >= all.size()) return new ArrayList<>();
        return new ArrayList<>(all.subList(start, end));
    }

    private void updatePager() {
        int startNum = all.isEmpty() ? 0 : pageIndex * PAGE_SIZE + 1;
        int endNum = Math.min((pageIndex + 1) * PAGE_SIZE, all.size());

        if (pageStatus != null) {
            pageStatus.setText(all.isEmpty() ? "0" : (startNum + "–" + endNum));
        }
        if (btnPrev != null) btnPrev.setVisibility(pageIndex > 0 ? android.view.View.VISIBLE : android.view.View.GONE);
        if (btnNext != null) btnNext.setVisibility(((pageIndex + 1) * PAGE_SIZE) < all.size() ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void updateTotalFooter(List<Expense> pageData) {
        double total = 0.0;
        for (Expense e : pageData) total += e.amount;

        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String code = prefs.getString("currency_code", "THB");
        String symbol = CurrencyUtils.symbolFor(code);

        DecimalFormat df = new DecimalFormat("#,##0.00");
        String formattedTotal = df.format(total) + " " + symbol;

        SpannableString totalDisplay = new SpannableString(formattedTotal);
        int start = formattedTotal.length() - symbol.length();
        totalDisplay.setSpan(new RelativeSizeSpan(0.85f), start, formattedTotal.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        if (tvTotalAmount != null) {
            tvTotalAmount.setText(totalDisplay);
        }
    }

    private Date parseDate(String raw) {
        String[] patterns = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "dd MMM yyyy",
                "dd MMM. yyyy"
        };
        for (String p : patterns) {
            try {
                SimpleDateFormat in = new SimpleDateFormat(p, Locale.ENGLISH);
                in.setLenient(false);
                return in.parse(raw);
            } catch (Exception ignore) {}
        }
        return null;
    }
}
