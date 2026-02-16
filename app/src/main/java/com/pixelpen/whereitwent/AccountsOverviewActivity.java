package com.pixelpen.whereitwent;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.PopupMenu;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsControllerCompat;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.content.Intent;
import android.text.Html;

import java.io.File;
import java.io.FileWriter;
import android.net.Uri;
import java.io.OutputStream;

import androidx.core.content.FileProvider;

import android.widget.Toast;





public class AccountsOverviewActivity extends AppCompatActivity {

    private static final String TAG_SECTION = "SECTION";
    private static final String TAG_ACCOUNT = "ACCOUNT";
    private static final String TAG_ITEM = "ITEM";

    private boolean showNotes = true;

    private long currentlyExpandedId = -1L;


    public static boolean needsRefresh = false;
    public static long expandAccountId = -1L;
    public static long filterAccountId = -1L;
    @Nullable
    public static String filterCategory = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        WindowCompat.setDecorFitsSystemWindows(getWindow(), true);

        if (savedInstanceState != null) {
            showNotes = savedInstanceState.getBoolean("show_notes", true);
        }

        setContentView(R.layout.activity_accounts_overview);

        expandAccountId =
                getIntent().getLongExtra("expand_account_id", -1L);

        ExpenseDatabase.migrateAccountsFromPrefsIfNeeded(this);

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        findViewById(R.id.btn_menu).setOnClickListener(v -> {

            PopupMenu popup = new PopupMenu(this, v);

            popup.getMenu().add("Filter Accounts");
            popup.getMenu().add("Manage Accounts");
            popup.getMenu().add("Show / Hide Notes");

            popup.getMenu().add("Export Accounts");


            popup.setOnMenuItemClickListener(item -> {

                String title = item.getTitle().toString();

                if ("Show / Hide Notes".equals(title)) {
                    showNotes = !showNotes;
                    rebuildAccounts();
                    return true;
                }

                if ("Manage Accounts".equals(title)) {
                    AddExpenseDialog dialog =
                            AddExpenseDialog.newManageAccountsInstance();
                    dialog.show(getSupportFragmentManager(), "MANAGE_ACCOUNTS");
                    return true;
                }

                if ("Export Accounts".equals(title)) {
                    showExportAccountsDialog();
                    return true;
                }


                if ("Filter Accounts".equals(title)) {

                    AccountCategoryFilterDialog dialog =
                            new AccountCategoryFilterDialog();

                    dialog.setListener((accountId, category) -> {
                        filterAccountId = accountId;
                        filterCategory = category;
                        rebuildAccounts();
                    });

                    dialog.show(getSupportFragmentManager(), "ACCOUNT_FILTER");
                    return true;
                }

                return false;
            });

            popup.show();
        });

        rebuildAccounts();
    }

    private void rebuildAccounts() {

        LinearLayout container = findViewById(R.id.accounts_container);
        container.removeAllViews();

        TextView filterIndicator =
                findViewById(R.id.text_filter_indicator);

        if (filterAccountId != -1L) {

            StringBuilder label = new StringBuilder("✕  ");


            if (filterCategory != null) {
                label.append(filterCategory);
            }


            filterIndicator.setText(label.toString());
            filterIndicator.setVisibility(View.VISIBLE);

            filterIndicator.setOnClickListener(v -> {
                filterAccountId = -1L;
                filterCategory = null;
                rebuildAccounts();
            });

        } else {
            filterIndicator.setVisibility(View.GONE);
            filterIndicator.setOnClickListener(null);
        }


        ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
        AccountDao accountDao = db.accountDao();
        AccountItemDao itemDao = db.accountItemDao();

        LayoutInflater inflater = LayoutInflater.from(this);

        List<AccountEntity> accounts = accountDao.getActiveAccounts();

        String[] ACCOUNT_TYPES = {"PROJECT", "TRAVEL", "CUSTOM"};

        Map<String, String> SECTION_LABELS = new HashMap<>();
        SECTION_LABELS.put("PROJECT", "PROJECTS");
        SECTION_LABELS.put("TRAVEL", "TRAVEL");
        SECTION_LABELS.put("CUSTOM", "CUSTOM");

        for (String type : ACCOUNT_TYPES) {

            if (filterAccountId == -1L) {

                boolean hasType = false;
                for (AccountEntity a : accounts) {
                    if (type.equalsIgnoreCase(a.type)) {
                        hasType = true;
                        break;
                    }
                }
                if (!hasType) continue;

                View section = addSection(inflater, container, type, SECTION_LABELS.get(type));
                section.setTag(TAG_SECTION);
            }

            for (AccountEntity account : accounts) {

                if (!type.equalsIgnoreCase(account.type)) continue;

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
                        CurrencyUtils.format(
                                displayTotal,
                                AppPrefs.getCurrencySymbol(this)
                        )
                );

                accountHeader.setTag(TAG_ACCOUNT);

                List<AccountItemEntity> items =
                        itemDao.getItemsForAccount(account.id);

                for (AccountItemEntity e : items) {

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
                                    CurrencyUtils.format(
                                            e.amount,
                                            AppPrefs.getCurrencySymbol(this)
                                    ),
                                    e.note
                            )
                    );

                    itemView.setTag(TAG_ITEM);

                    // Default collapsed
                    itemView.setVisibility(View.GONE);

                    // Expand only if filtered or explicitly requested
                    if (filterAccountId != -1L ||
                            account.id == expandAccountId ||
                            account.id == currentlyExpandedId) {
                        itemView.setVisibility(View.VISIBLE);
                    }


                    itemView.setOnLongClickListener(v -> {

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

                        return true;
                    });

                }

                if (filterAccountId == -1L) {
                    accountHeader
                            .setOnClickListener(v -> {

                                if (currentlyExpandedId == account.id) {
                                    currentlyExpandedId = -1L;
                                } else {
                                    currentlyExpandedId = account.id;
                                }

                                rebuildAccounts();
                            });

                }
            }
        }
    }

    // --- helpers unchanged ---

    private View addSection(LayoutInflater inflater,
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
        return v;
    }

    private View addAccountHeader(LayoutInflater inflater,
                                  LinearLayout container,
                                  String name,
                                  String total) {

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

    private View addAccountItem(LayoutInflater inflater,
                                LinearLayout container,
                                AccountItem item) {

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

        block.addView(header);

        if (item.note != null &&
                !item.note.trim().isEmpty() &&
                showNotes) {

            View noteRow = inflater.inflate(
                    R.layout.row_account_item_note,
                    block,
                    false
            );

            TextView noteView =
                    noteRow.findViewById(R.id.text_note);

            noteView.setText("Note: " + item.note);
            block.addView(noteRow);
        }

        container.addView(block);
        return block;
    }

    private String capitalize(String s) {
        if (s == null || s.isEmpty()) return "";
        return s.substring(0, 1).toUpperCase()
                + s.substring(1);
    }


    private void toggleItems(LinearLayout container,
                             int headerIndex) {

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

    private void showExportAccountsDialog() {

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Export Accounts")
                .setMessage("Choose export method:")
                .setPositiveButton("STORAGE (CSV)", (d, w) -> exportAccountsCsv())
                .setNeutralButton("EMAIL (HTML)", (d, w) -> exportAccountsHtml())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void exportAccountsCsv() {

        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.setType("text/csv");
        intent.putExtra(Intent.EXTRA_TITLE, "where_it_went_accounts.csv");

        startActivityForResult(intent, 9001);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 9001 && resultCode == RESULT_OK && data != null) {

            Uri uri = data.getData();
            if (uri == null) return;

            try (OutputStream os = getContentResolver().openOutputStream(uri)) {

                ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
                AccountDao accountDao = db.accountDao();
                AccountItemDao itemDao = db.accountItemDao();

                List<AccountEntity> accounts = accountDao.getAllAccounts();

                StringBuilder sb = new StringBuilder();
                sb.append("AccountType,AccountName,Date,Item,Category,Amount,Note\n");

                for (AccountEntity account : accounts) {

                    List<AccountItemEntity> items =
                            itemDao.getItemsForAccount(account.id);

                    for (AccountItemEntity item : items) {

                        sb.append(account.type).append(",");

                        sb.append(account.name);
                        if (account.archived) {
                            sb.append(" (archived)");
                        }
                        sb.append(",");

                        sb.append(item.date).append(",");
                        sb.append(item.item).append(",");
                        sb.append(item.category).append(",");
                        sb.append(item.amount).append(",");
                        sb.append(item.note == null ? "" :
                                item.note.replace(",", " "));
                        sb.append("\n");
                    }
                }

                os.write(sb.toString().getBytes());
                os.flush();

                Toast.makeText(this,
                        "Accounts exported successfully",
                        Toast.LENGTH_SHORT).show();

            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(this,
                        "Export failed",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }



    private void exportAccountsHtml() {

        try {

            ExpenseDatabase db = ExpenseDatabase.getDatabase(this);
            AccountDao accountDao = db.accountDao();
            AccountItemDao itemDao = db.accountItemDao();

            List<AccountEntity> accounts = accountDao.getAllAccounts();

            StringBuilder html = new StringBuilder();
            html.append("<html><body>");
            html.append("<h1>Accounts Export</h1>");

            String currentType = "";

            for (AccountEntity account : accounts) {

                if (!account.type.equals(currentType)) {
                    currentType = account.type;
                    html.append("<h2>").append(currentType).append("</h2>");
                }

                html.append("<h3>")
                        .append(account.name);

                if (account.archived) {
                    html.append(" (archived)");
                }

                html.append("</h3>");

                html.append("<table border='1' cellpadding='4' cellspacing='0'>");
                html.append("<tr><th>Date</th><th>Item</th><th>Category</th><th>Amount</th><th>Note</th></tr>");

                List<AccountItemEntity> items =
                        itemDao.getItemsForAccount(account.id);

                for (AccountItemEntity item : items) {
                    html.append("<tr>");
                    html.append("<td>").append(item.date).append("</td>");
                    html.append("<td>").append(item.item).append("</td>");
                    html.append("<td>").append(item.category).append("</td>");
                    html.append("<td>").append(item.amount).append("</td>");
                    html.append("<td>").append(item.note == null ? "" : item.note).append("</td>");
                    html.append("</tr>");
                }

                html.append("</table><br>");
            }

            html.append("</body></html>");

            File file = new File(getExternalFilesDir(null), "where_it_went_accounts.html");

            FileWriter writer = new FileWriter(file);
            writer.write(html.toString());
            writer.close();

            Uri uri = FileProvider.getUriForFile(
                    this,
                    getPackageName() + ".fileprovider",
                    file
            );


            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/html");
            intent.putExtra(Intent.EXTRA_SUBJECT, "Where It Went - Accounts Export");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(intent, "Send email"));

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}