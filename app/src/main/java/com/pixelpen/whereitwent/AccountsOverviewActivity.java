package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AccountsOverviewActivity extends AppCompatActivity {

    private static final String TAG_SECTION = "SECTION";
    private static final String TAG_ACCOUNT = "ACCOUNT";
    private static final String TAG_ITEM = "ITEM";

    public static boolean needsRefresh = false;
    public static long forceExpandAccountId = -1L;

    public static long filterAccountId = -1L;
    @Nullable
    public static String filterCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_overview);

        String currencySymbol = AppPrefs.getCurrencySymbol(this);
        long expandAccountId;

        if (filterAccountId != -1L) {
            // Filter active → always expand the filtered account
            expandAccountId = filterAccountId;
        } else {
            expandAccountId =
                    forceExpandAccountId != -1L
                            ? forceExpandAccountId
                            : getIntent().getLongExtra("expand_account_id", -1L);
        }

        forceExpandAccountId = -1L;

        ExpenseDatabase.migrateAccountsFromPrefsIfNeeded(this);
        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_back).setOnLongClickListener(v -> {

            AccountCategoryFilterDialog dialog =
                    new AccountCategoryFilterDialog();

            dialog.setListener((accountId, category) -> {
                filterAccountId = accountId;
                filterCategory = category;

                findViewById(R.id.accounts_container)
                        .post(this::recreate);
            });

            dialog.show(
                    getSupportFragmentManager(),
                    "ACCOUNT_FILTER"
            );
            return true;
        });


        ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
        AccountDao accountDao = db.accountDao();
        AccountItemDao itemDao = db.accountItemDao();

        LinearLayout container = findViewById(R.id.accounts_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        List<AccountEntity> accounts = accountDao.getActiveAccounts();

        String[] ACCOUNT_TYPES = {"PROJECT", "TRAVEL", "CUSTOM"};

        Map<String, String> SECTION_LABELS = new HashMap<>();
        SECTION_LABELS.put("PROJECT", "PROJECTS");
        SECTION_LABELS.put("TRAVEL", "TRAVEL");
        SECTION_LABELS.put("CUSTOM", "CUSTOM");

        for (String type : ACCOUNT_TYPES) {

            // FILTER ACTIVE → skip section headers entirely
            if (filterAccountId != -1L) {
                // We will render only the filtered account below
            } else {

                boolean hasType = false;
                for (AccountEntity a : accounts) {
                    if (type.equalsIgnoreCase(a.type)) {
                        hasType = true;
                        break;
                    }
                }
                if (!hasType) continue;

                View section = addSection(
                        inflater,
                        container,
                        type,
                        SECTION_LABELS.get(type)
                );
                section.setTag(TAG_SECTION);
            }


            for (AccountEntity account : accounts) {

                if (!type.equalsIgnoreCase(account.type)) continue;

                // FILTER — ACCOUNT (STRUCTURAL)
                if (filterAccountId != -1L &&
                        account.id != filterAccountId) {
                    continue;
                }

                double displayTotal = 0.0;

                List<AccountItemEntity> totalItems =
                        itemDao.getItemsForAccount(account.id);

                for (AccountItemEntity e : totalItems) {

                    if (filterCategory != null &&
                            !filterCategory.equals(e.category)) {
                        continue;
                    }

                    displayTotal += e.amount;
                }

                View accountHeader = addAccountHeader(
                        inflater,
                        container,
                        account.name,
                        CurrencyUtils.format(displayTotal, "฿")
                );

                accountHeader.setTag(TAG_ACCOUNT);

                List<AccountItemEntity> items =
                        itemDao.getItemsForAccount(account.id);

                for (AccountItemEntity e : items) {

                    // FILTER — ACCOUNT
                    if (filterAccountId != -1L &&
                            account.id != filterAccountId) {
                        continue;
                    }

                    // FILTER — CATEGORY
                    if (filterCategory != null &&
                            !filterCategory.equals(e.category)) {
                        continue;
                    }

                    View itemView = addAccountItem(
                            inflater,
                            container,
                            new AccountItem(
                                    e.dateMillis,
                                    capitalize(e.item),
                                    e.category,
                                    CurrencyUtils.format(e.amount, currencySymbol),
                                    e.note
                            )
                    );

                    itemView.setTag(TAG_ITEM);
                    if (filterAccountId != -1L) {
                        // Filtered view: always expanded
                        itemView.setVisibility(View.VISIBLE);
                    } else {
                        // Normal view: respect expand/collapse
                        itemView.setVisibility(
                                account.id == expandAccountId
                                        ? View.VISIBLE
                                        : View.GONE
                        );
                    }


                    itemView.setOnClickListener(v -> {
                        EditAccountItemDialog dialog =
                                new EditAccountItemDialog();

                        Bundle args = new Bundle();
                        args.putLong(
                                EditAccountItemDialog.ARG_ACCOUNT_ITEM_ID,
                                e.id
                        );
                        dialog.setArguments(args);

                        dialog.show(
                                getSupportFragmentManager(),
                                "EDIT_ACCOUNT_ITEM"
                        );
                    });
                }
                if (filterAccountId == -1L) {

                    accountHeader.setOnClickListener(v -> {
                        int idx = container.indexOfChild(accountHeader);
                        toggleItems(container, idx);
                    });

                    if (account.id == expandAccountId) {
                        container.post(() -> {
                            int idx = container.indexOfChild(accountHeader);
                            toggleItems(container, idx);
                        });
                    }
                }

            }
        }
    }

    private View addSection(
            LayoutInflater inflater,
            LinearLayout container,
            String type,
            String title
    ) {

        int layoutRes =
                "TRAVEL".equalsIgnoreCase(type)
                        ? R.layout.row_account_section_amber
                        : R.layout.row_account_section_blue;

        View v = inflater.inflate(layoutRes, container, false);
        TextView t = v.findViewById(R.id.text_title);
        t.setText(title);
        container.addView(v);
        return v;
    }

    private View addAccountHeader(
            LayoutInflater inflater,
            LinearLayout container,
            String name,
            String total
    ) {

        View v = inflater.inflate(
                R.layout.row_account_item,
                container,
                false
        );

        TextView nameView = v.findViewById(R.id.text_account_name);
        TextView totalView = v.findViewById(R.id.text_account_total);

        nameView.setText(name);
        totalView.setText(total);

        container.addView(v);
        return v;
    }

    private View addAccountItem(
            LayoutInflater inflater,
            LinearLayout container,
            AccountItem item
    ) {

        LinearLayout block = new LinearLayout(this);
        block.setOrientation(LinearLayout.VERTICAL);

        View header = inflater.inflate(
                R.layout.row_account_item_header,
                block,
                false
        );

        TextView monthView = header.findViewById(R.id.text_month);
        TextView dayView = header.findViewById(R.id.text_day);
        TextView itemView = header.findViewById(R.id.text_item);
        TextView categoryView = header.findViewById(R.id.text_category);
        TextView amountView = header.findViewById(R.id.text_amount);

        SimpleDateFormat monthFmt =
                new SimpleDateFormat("MMM", Locale.ENGLISH);
        SimpleDateFormat dayFmt =
                new SimpleDateFormat("dd", Locale.ENGLISH);

        Date d = new Date(item.dateMillis);

        monthView.setText(monthFmt.format(d));
        dayView.setText(dayFmt.format(d));

        itemView.setText(item.item);
        categoryView.setText(item.category);
        amountView.setText(item.amount);

        View noteRow = inflater.inflate(
                R.layout.row_account_item_note,
                block,
                false
        );

        TextView noteView = noteRow.findViewById(R.id.text_note);
        noteView.setText("Note: " + item.note);
        noteView.setTypeface(
                noteView.getTypeface(),
                android.graphics.Typeface.ITALIC
        );

        block.addView(header);
        block.addView(noteRow);

        container.addView(block);
        return block;
    }

    private void toggleItems(
            LinearLayout container,
            int headerIndex
    ) {

        boolean hide;

        if (headerIndex + 1 < container.getChildCount()) {
            View next = container.getChildAt(headerIndex + 1);
            hide = next.getVisibility() == View.VISIBLE;
        } else {
            hide = false;
        }

        for (int i = headerIndex + 1;
             i < container.getChildCount();
             i++) {

            View v = container.getChildAt(i);
            Object tag = v.getTag();

            if (TAG_ACCOUNT.equals(tag)
                    || TAG_SECTION.equals(tag)) {
                break;
            }

            v.setVisibility(
                    hide ? View.GONE : View.VISIBLE
            );
        }
    }

    private double safe(Double d) {
        return d == null ? 0.0 : d;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase()
                + s.substring(1);
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (needsRefresh) {
            needsRefresh = false;
            recreate();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        // Clear filter ONLY when leaving the screen for real
        if (isFinishing()) {
            filterAccountId = -1L;
            filterCategory = null;
        }
    }
}