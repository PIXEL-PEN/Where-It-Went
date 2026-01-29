package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AccountsOverviewActivity extends AppCompatActivity {

    private static final String TAG_SECTION = "SECTION";
    private static final String TAG_ACCOUNT = "ACCOUNT";
    private static final String TAG_ITEM    = "ITEM";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_overview);

        long expandAccountId =
                getIntent().getLongExtra("expand_account_id", -1L);

        ExpenseDatabase.migrateAccountsFromPrefsIfNeeded(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

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

            for (AccountEntity account : accounts) {

                if (!type.equalsIgnoreCase(account.type)) continue;

                View accountHeader = addAccountHeader(
                        inflater,
                        container,
                        account.name,
                        CurrencyUtils.format(
                                safe(itemDao.getTotalForAccount(account.id)),
                                "฿"
                        )
                );
                accountHeader.setTag(TAG_ACCOUNT);

                List<AccountItemEntity> items =
                        itemDao.getItemsForAccount(account.id);

                for (AccountItemEntity e : items) {

                    View itemView = addAccountItem(
                            inflater,
                            container,
                            new AccountItem(
                                    e.date,
                                    capitalize(e.item),
                                    e.category,
                                    CurrencyUtils.format(e.amount, "฿"),
                                    e.note
                            )
                    );
                    itemView.setTag(TAG_ITEM);
                    itemView.setVisibility(
                            account.id == expandAccountId
                                    ? View.VISIBLE
                                    : View.GONE
                    );

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

        TextView nameView  = v.findViewById(R.id.text_account_name);
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

        TextView monthView    = header.findViewById(R.id.text_month);
        TextView dayView      = header.findViewById(R.id.text_day);
        TextView itemView     = header.findViewById(R.id.text_item);
        TextView categoryView = header.findViewById(R.id.text_category);
        TextView amountView   = header.findViewById(R.id.text_amount);

        String[] parts = item.date.split(" ");
        if (parts.length == 2) {
            monthView.setText(parts[0]);
            dayView.setText(parts[1]);
        }

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
}
