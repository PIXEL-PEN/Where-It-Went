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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_accounts_overview);

        ExpenseDatabase.migrateAccountsFromPrefsIfNeeded(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
        AccountDao accountDao = db.accountDao();
        AccountItemDao itemDao = db.accountItemDao();

        LinearLayout container = findViewById(R.id.accounts_container);
        LayoutInflater inflater = LayoutInflater.from(this);

        List<AccountEntity> accounts = accountDao.getActiveAccounts();

        String[] ACCOUNT_TYPES = {
                "PROJECT",
                "TRAVEL",
                "CUSTOM"
        };

        Map<String, String> SECTION_LABELS = new HashMap<>();
        SECTION_LABELS.put("PROJECT", "PROJECTS");
        SECTION_LABELS.put("TRAVEL",  "TRAVEL");
        SECTION_LABELS.put("CUSTOM",  "CUSTOM");

        for (String type : ACCOUNT_TYPES) {

            boolean hasType = false;

            for (AccountEntity account : accounts) {
                if (type.equalsIgnoreCase(account.type)) {
                    hasType = true;
                    break;
                }
            }

            if (!hasType) continue;

            addSection(
                    inflater,
                    container,
                    type,
                    SECTION_LABELS.get(type)
            );

            for (AccountEntity account : accounts) {

                if (!type.equalsIgnoreCase(account.type)) continue;

                android.util.Log.d(
                        "ACCOUNTS",
                        "Account: " + account.name + " id=" + account.id
                );

                LinearLayout accountBlock = new LinearLayout(this);
                accountBlock.setOrientation(LinearLayout.VERTICAL);
                container.addView(accountBlock);

                View accountHeader = addProjectHeader(
                        inflater,
                        accountBlock,
                        account.name,
                        CurrencyUtils.format(
                                safe(itemDao.getTotalForAccount(account.id)),
                                "฿"
                        )
                );


                LinearLayout accountItems = new LinearLayout(this);
                accountItems.setOrientation(LinearLayout.VERTICAL);
                accountItems.setVisibility(View.VISIBLE);
                accountBlock.addView(accountItems);

                accountHeader.setOnClickListener(v -> {
                    accountItems.setVisibility(
                            accountItems.getVisibility() == View.VISIBLE
                                    ? View.GONE
                                    : View.VISIBLE
                    );
                });

                List<AccountItemEntity> items =
                        itemDao.getItemsForAccount(account.id);


                android.util.Log.d(
                        "ACCOUNTS",
                        "Items for account " + account.id + ": " + items.size()
                );



                for (AccountItemEntity e : items) {
                    addProjectItem(
                            inflater,
                            accountItems,
                            new AccountItem(
                                    e.date,
                                    capitalize(e.item),
                                    e.category,
                                    CurrencyUtils.format(e.amount, "฿"),
                                    e.note
                            )
                    );
                }
            }
        }
    }

    private void addSection(LayoutInflater inflater,
                            LinearLayout container,
                            String type,
                            String title) {

        int layoutRes =
                "TRAVEL".equalsIgnoreCase(type)
                        ? R.layout.row_account_section_amber
                        : R.layout.row_account_section_blue;

        View v = inflater.inflate(layoutRes, container, false);
        TextView t = v.findViewById(R.id.text_title);
        t.setText(title);
        container.addView(v);
    }

    private View addProjectHeader(LayoutInflater inflater,
                                  LinearLayout container,
                                  String name,
                                  String total) {

        View v = inflater.inflate(R.layout.row_account_item, container, false);

        TextView nameView  = v.findViewById(R.id.text_account_name);
        TextView totalView = v.findViewById(R.id.text_account_total);

        nameView.setText(name);
        totalView.setText(total);

        container.addView(v);
        return v;
    }

    private void addProjectItem(LayoutInflater inflater,
                                LinearLayout projectItems,
                                AccountItem item) {

        LinearLayout itemBlock = new LinearLayout(this);
        itemBlock.setOrientation(LinearLayout.VERTICAL);

        View header = inflater.inflate(
                R.layout.row_account_item_header,
                itemBlock,
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
                itemBlock,
                false
        );

        TextView noteView = noteRow.findViewById(R.id.text_note);
        noteView.setText("Note: " + item.note);
        noteView.setTypeface(
                noteView.getTypeface(),
                android.graphics.Typeface.ITALIC
        );

        itemBlock.addView(header);
        itemBlock.addView(noteRow);

        projectItems.addView(itemBlock);
    }

    private double safe(Double d) {
        return d == null ? 0.0 : d;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase() + s.substring(1);
    }
}
