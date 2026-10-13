package com.pixelpen.whereitwent;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Yearly Summary (v3.1).
 *
 * One banner per 12-month period, anchored to the user's start month
 * (Jan–Dec for a January starter, May–Apr for a May starter), newest first.
 * The banner shows the period's total; tapping it shows or hides the
 * category totals for Daily Living, and the list of accounts. Each account
 * row shows its total and toggles that account's own category totals.
 */
public class YearlySummaryActivity extends AppCompatActivity {

    private static final int COLOR_TEXT = 0xFF444444;
    private static final int COLOR_SECTION_TEXT = 0xFF4F6B7A;
    private static final int COLOR_BANNER_SUB = 0xFFB9CFDB;

    // Expanded periods ("yyyy-MM" end month) and accounts ("yyyy-MM|accountId");
    // kept across refreshes while the screen is open
    private final Set<String> expanded = new HashSet<>();
    private boolean firstBuild = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_yearly_summary);

        ImageButton btnBack = findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                startActivity(new Intent(this, MainScreen.class));
                finish();
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        build();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        SummaryExport.onActivityResult(this, requestCode, resultCode, data);
    }

    // --------------------------------------------------------
    // BUILD
    // --------------------------------------------------------
    private void build() {
        LinearLayout container = findViewById(R.id.summary_container);
        container.removeAllViews();

        ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
        List<Expense> expenses = db.expenseDao().getAll();
        List<AccountItemEntity> items = db.accountItemDao().getAllItems();
        List<AccountEntity> accounts = db.accountDao().getAllAccounts();

        String nowYm = CategorySummary.currentYm();
        String sym = AppPrefs.getCurrencySymbol(this);
        List<String> ends = CategorySummary.anchoredPeriodEnds(expenses, items, nowYm);

        // Newest period starts open the first time the screen is shown
        if (firstBuild && !ends.isEmpty()) {
            expanded.add(ends.get(0));
            firstBuild = false;
        }

        for (String endYm : ends) {
            CategorySummary s = CategorySummary.build(expenses, items, accounts, endYm);
            addPeriod(container, s, nowYm, sym);
        }
    }

    private void addPeriod(LinearLayout container, CategorySummary s, String nowYm, String sym) {

        // ---------- BANNER ----------
        LinearLayout banner = new LinearLayout(this);
        banner.setOrientation(LinearLayout.HORIZONTAL);
        banner.setGravity(Gravity.CENTER_VERTICAL);
        banner.setBackgroundColor(ContextCompat.getColor(this, R.color.colorPrimary));
        banner.setPadding(dp(14), dp(10), dp(12), dp(10));
        banner.setClickable(true);
        banner.setFocusable(true);

        LinearLayout.LayoutParams bannerLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bannerLp.setMargins(dp(12), dp(10), dp(12), 0);

        LinearLayout left = new LinearLayout(this);
        left.setOrientation(LinearLayout.VERTICAL);
        left.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        TextView period = text(s.periodLabel(), 16, 0xFFFFFFFF, true);
        TextView status = text(s.statusLabel(nowYm), 12, COLOR_BANNER_SUB, false);
        left.addView(period);
        left.addView(status);

        TextView total = text(CurrencyUtils.format(s.grandTotal(), sym), 16, 0xFFFFFFFF, true);
        total.setGravity(Gravity.END);

        boolean open = expanded.contains(s.endYm);
        TextView arrow = text(open ? "▲" : "▼", 14, 0xFFFFFFFF, false);
        arrow.setPadding(dp(10), 0, 0, 0);

        banner.addView(left);
        banner.addView(total);
        banner.addView(arrow);
        banner.setContentDescription(s.periodLabel() + ", " + s.statusLabel(nowYm)
                + ", total " + CurrencyUtils.format(s.grandTotal(), sym));
        container.addView(banner, bannerLp);

        // ---------- CATEGORIES (toggled) ----------
        LinearLayout body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setBackgroundColor(0xFFFFFFFF);
        body.setPadding(0, 0, 0, dp(6));
        body.setVisibility(open ? View.VISIBLE : View.GONE);

        LinearLayout.LayoutParams bodyLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        bodyLp.setMargins(dp(12), 0, dp(12), 0);

        addSection(body, "Daily Living", s.dailyLiving, s.dailyLivingTotal,
                R.color.header_bg_blue, sym);
        addAccountsSection(body, s, sym);
        addExportButton(body, s);

        container.addView(body, bodyLp);

        banner.setOnClickListener(v -> {
            boolean nowOpen = body.getVisibility() != View.VISIBLE;
            body.setVisibility(nowOpen ? View.VISIBLE : View.GONE);
            arrow.setText(nowOpen ? "▲" : "▼");
            if (nowOpen) expanded.add(s.endYm); else expanded.remove(s.endYm);
        });
    }

    private void addSection(LinearLayout body, String title, Map<String, Double> rows,
                            double total, int bgColorRes, String sym) {

        LinearLayout header = row(title, CurrencyUtils.format(total, sym),
                COLOR_SECTION_TEXT, true, dp(14));
        header.setBackgroundColor(ContextCompat.getColor(this, bgColorRes));
        header.setPadding(dp(14), dp(7), dp(14), dp(7));
        body.addView(header);

        if (rows.isEmpty()) {
            TextView none = text("No records in this period", 13, 0xFF888888, false);
            none.setTypeface(null, Typeface.ITALIC);
            none.setPadding(dp(26), dp(8), dp(14), dp(8));
            body.addView(none);
            return;
        }

        boolean first = true;
        for (Map.Entry<String, Double> en : rows.entrySet()) {
            if (!first) body.addView(divider());
            first = false;
            LinearLayout r = row(en.getKey(), CurrencyUtils.format(en.getValue(), sym),
                    COLOR_TEXT, false, dp(26));
            r.setPadding(dp(26), dp(7), dp(14), dp(7));
            body.addView(r);
        }
    }

    private void addAccountsSection(LinearLayout body, CategorySummary s, String sym) {

        LinearLayout header = row("Accounts", CurrencyUtils.format(s.accountsTotal, sym),
                COLOR_SECTION_TEXT, true, dp(14));
        header.setBackgroundColor(ContextCompat.getColor(this, R.color.header_bg_amber));
        header.setPadding(dp(14), dp(7), dp(14), dp(7));
        body.addView(header);

        if (s.accounts.isEmpty()) {
            TextView none = text("No records in this period", 13, 0xFF888888, false);
            none.setTypeface(null, Typeface.ITALIC);
            none.setPadding(dp(26), dp(8), dp(14), dp(8));
            body.addView(none);
            return;
        }

        boolean first = true;
        for (CategorySummary.AccountGroup g : s.accounts) {
            if (!first) body.addView(divider());
            first = false;
            addAccount(body, s, g, sym);
        }
    }

    /** One account: a toggle row with its total, then its categories when open. */
    private void addAccount(LinearLayout body, CategorySummary s,
                            CategorySummary.AccountGroup g, String sym) {

        String key = s.endYm + "|" + g.accountId;
        boolean open = expanded.contains(key);

        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(dp(26), dp(8), dp(14), dp(8));
        r.setClickable(true);
        r.setFocusable(true);

        TextView name = text(g.displayName(), 14, COLOR_TEXT, true);
        name.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView total = text(CurrencyUtils.format(g.total(), sym), 14, COLOR_TEXT, true);
        total.setGravity(Gravity.END);
        TextView arrow = text(open ? "▲" : "▼", 11, COLOR_SECTION_TEXT, false);
        arrow.setPadding(dp(8), 0, 0, 0);

        r.addView(name);
        r.addView(total);
        r.addView(arrow);
        if (g.archived) r.setAlpha(0.55f);   // matches dimmed archived accounts elsewhere
        r.setContentDescription(g.displayName() + ", total "
                + CurrencyUtils.format(g.total(), sym));
        body.addView(r);

        LinearLayout cats = new LinearLayout(this);
        cats.setOrientation(LinearLayout.VERTICAL);
        cats.setVisibility(open ? View.VISIBLE : View.GONE);
        if (g.archived) cats.setAlpha(0.55f);
        for (Map.Entry<String, Double> en : g.categories.entrySet()) {
            LinearLayout c = row(en.getKey(), CurrencyUtils.format(en.getValue(), sym),
                    COLOR_TEXT, false, dp(44));
            c.setPadding(dp(44), dp(5), dp(14 + 19), dp(5)); // amounts line up under the account total
            cats.addView(c);
        }
        body.addView(cats);

        r.setOnClickListener(v -> {
            boolean nowOpen = cats.getVisibility() != View.VISIBLE;
            cats.setVisibility(nowOpen ? View.VISIBLE : View.GONE);
            arrow.setText(nowOpen ? "▲" : "▼");
            if (nowOpen) expanded.add(key); else expanded.remove(key);
        });
    }

    private void addExportButton(LinearLayout body, CategorySummary s) {
        TextView btn = text("Export", 14, 0xFFFFFFFF, false);
        btn.setGravity(Gravity.CENTER);
        btn.setBackgroundColor(ContextCompat.getColor(this, R.color.colorAccent2));
        btn.setPadding(dp(18), dp(6), dp(18), dp(6));
        btn.setClickable(true);
        btn.setFocusable(true);
        btn.setContentDescription("Export " + s.periodLabel());
        btn.setOnClickListener(v -> SummaryExport.show(this, s));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.END;
        lp.setMargins(0, dp(8), dp(14), dp(4));
        body.addView(btn, lp);
    }

    // --------------------------------------------------------
    // VIEW HELPERS
    // --------------------------------------------------------
    private LinearLayout row(String label, String value, int color, boolean bold, int padStart) {
        LinearLayout r = new LinearLayout(this);
        r.setOrientation(LinearLayout.HORIZONTAL);
        r.setGravity(Gravity.CENTER_VERTICAL);
        r.setPadding(padStart, dp(7), dp(14), dp(7));

        TextView l = text(label, 14, color, bold);
        l.setLayoutParams(new LinearLayout.LayoutParams(0,
                LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        TextView v = text(value, 14, color, bold);
        v.setGravity(Gravity.END);

        r.addView(l);
        r.addView(v);
        return r;
    }

    private View divider() {
        View d = new View(this);
        d.setBackgroundColor(ContextCompat.getColor(this, R.color.dividerGray));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, Math.max(1, dp(1) / 2));
        lp.setMargins(dp(26), 0, dp(14), 0);
        d.setLayoutParams(lp);
        return d;
    }

    private TextView text(String s, int sp, int color, boolean bold) {
        TextView tv = new TextView(this);
        tv.setText(s);
        tv.setTextSize(sp);
        tv.setTextColor(color);
        if (bold) tv.setTypeface(null, Typeface.BOLD);
        return tv;
    }

    private int dp(int v) {
        return Math.round(v * getResources().getDisplayMetrics().density);
    }
}
