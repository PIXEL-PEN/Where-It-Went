package com.pixelpen.whereitwent;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ImportTest {

    // ---------- helpers ----------

    private static Expense exp(String iso, String cat, String item, double amt) {
        Expense e = new Expense();
        e.date = iso; e.category = cat; e.description = item; e.amount = amt;
        return e;
    }

    private static AccountEntity acct(long id, String name) {
        AccountEntity a = new AccountEntity(name, "Project", false);
        a.id = id;
        return a;
    }

    private static AccountItemEntity aitem(long acctId, String date, String item, String cat, double amt) {
        return new AccountItemEntity(acctId, date, 0, item, cat, amt, null);
    }

    private static final Map<String, String> DEFAULT_CATS = new LinkedHashMap<>();
    static {
        for (String c : Arrays.asList("Groceries", "Rent", "Utilities", "Bills", "Transport", "Other")) {
            DEFAULT_CATS.put(c, "Fixed");
        }
    }

    // ---------- Daily Living CSV ----------

    @Test
    public void oldDailyLivingExportWithCommasAndNulls() {
        String csv = "Category,Item,Amount,Date\n"
                + "Groceries,Milk,45.0,03 Jan 2026\n"
                + "Groceries,Bread, eggs, butter,120.5,04 Jan 2026\n"   // commas inside item
                + "Bills,null,1.0E7,05 Jan 2026\n"                     // null item, huge amount
                + "Pets,Cat food,300.0,13 Oct 2026\r\n"                // CRLF line ending
                + "Garbage line\n"                                     // unreadable
                + "\n";
        BackupFormat.Parsed p = BackupFormat.parse(csv);

        assertEquals(BackupFormat.Kind.DAILY_LIVING_CSV, p.kind);
        assertEquals(4, p.expenses.size());
        assertEquals("Bread, eggs, butter", p.expenses.get(1).item);
        assertEquals(120.5, p.expenses.get(1).amount, 0.0001);
        assertEquals("", p.expenses.get(2).item);
        assertEquals(1.0E7, p.expenses.get(2).amount, 0.0001);
        assertEquals("2026-10-13", p.expenses.get(3).isoDate);
        assertEquals(Collections.singletonList(6), p.badLines);
    }

    @Test
    public void quotedDailyLivingExport() {
        String csv = "﻿Category,Item,Amount,Date\n"
                + "Groceries,\"Bread, \"\"fresh\"\"\",120.5,04 Jan 2026\n";
        BackupFormat.Parsed p = BackupFormat.parse(csv);
        assertEquals(1, p.expenses.size());
        assertEquals("Bread, \"fresh\"", p.expenses.get(0).item);
    }

    // ---------- Accounts CSV ----------

    @Test
    public void oldAccountsExportWithCommasAndArchived() {
        String csv = "AccountType,AccountName,Date,Item,Category,Amount,Note\n"
                + "Project,Condo build,05 Feb 2026,Tiles, grout,Materials,1200.0,paid cash\n"
                + "Travel,Trip, Chiang Mai (archived),10 Mar 2026,Hotel,Lodging,5638.0,\n"
                + "Project,Condo build,06 Feb 2026,null,Labour,800.0,\n";
        BackupFormat.Parsed p = BackupFormat.parse(csv);

        assertEquals(BackupFormat.Kind.ACCOUNTS_CSV, p.kind);
        assertTrue(p.badLines.isEmpty());
        assertEquals(2, p.accounts.size());
        assertEquals("Condo build", p.accounts.get(0).name);
        assertEquals("Trip, Chiang Mai", p.accounts.get(1).name);
        assertTrue(p.accounts.get(1).archived);

        assertEquals(3, p.accountItems.size());
        BackupFormat.AccountItemRec first = p.accountItems.get(0);
        assertEquals("Tiles, grout", first.item);
        assertEquals("Materials", first.category);
        assertEquals(1200.0, first.amount, 0.0001);
        assertEquals("paid cash", first.note);
        assertEquals("2026-02-05", first.isoDate);
        assertEquals("", p.accountItems.get(2).item);
    }

    // ---------- Other files ----------

    @Test
    public void summaryAndUnknownFilesAreRecognised() {
        assertEquals(BackupFormat.Kind.SUMMARY_CSV,
                BackupFormat.parse("Category Summary,Jan – Oct 2026,In progress\nSection,Account,Category,Total\n").kind);
        assertEquals(BackupFormat.Kind.UNKNOWN, BackupFormat.parse("<html><body>").kind);
        assertEquals(BackupFormat.Kind.UNKNOWN, BackupFormat.parse("").kind);
    }

    // ---------- Full backup ----------

    @Test
    public void fullBackupRoundTrip() {
        Map<String, String> cats = new LinkedHashMap<>();
        cats.put("Pets", "Discretionary");
        cats.put("Off-Budget", "Off-Budget");
        List<BackupFormat.AccountRec> accounts = Arrays.asList(
                new BackupFormat.AccountRec("Condo, phase 2", "Project", false),
                new BackupFormat.AccountRec("Old trip", "Travel", true),
                new BackupFormat.AccountRec("Empty one", "Custom", false));
        List<BackupFormat.ExpenseRec> expenses = Arrays.asList(
                new BackupFormat.ExpenseRec("2026-01-03", "Pets", "Food, \"premium\"", 300.25),
                new BackupFormat.ExpenseRec("2026-01-04", "Rent", "", 1.0E7));
        List<BackupFormat.AccountItemRec> items = Collections.singletonList(
                new BackupFormat.AccountItemRec("Condo, phase 2", "2026-02-05", "Tiles",
                        "Materials", 1200, "line1, with comma"));

        String text = BackupFormat.writeBackup("฿", cats, accounts, expenses, items, new Date());
        assertTrue(text.contains("EXPENSE,2026-01-04,Rent,,10000000\n"));

        BackupFormat.Parsed p = BackupFormat.parse(text);
        assertEquals(BackupFormat.Kind.FULL_BACKUP, p.kind);
        assertTrue(p.badLines.isEmpty());
        assertEquals("฿", p.currencySymbol);
        assertEquals(cats, p.categories);
        assertEquals(3, p.accounts.size());
        assertTrue(p.accounts.get(1).archived);
        assertEquals("Condo, phase 2", p.accounts.get(0).name);
        assertEquals("Food, \"premium\"", p.expenses.get(0).item);
        assertEquals(300.25, p.expenses.get(0).amount, 0.0001);
        assertEquals("line1, with comma", p.accountItems.get(0).note);
        assertEquals("Condo, phase 2", p.accountItems.get(0).accountName);
    }

    // ---------- Import plan ----------

    @Test
    public void importIntoEmptyAppAddsEverything() {
        BackupFormat.Parsed p = BackupFormat.parse("Category,Item,Amount,Date\n"
                + "Groceries,Milk,45.0,03 Jan 2026\n"
                + "Pets,Cat food,300.0,04 Jan 2026\n");
        ImportPlan plan = ImportPlan.build(p, Collections.<Expense>emptyList(), null, null,
                DEFAULT_CATS, "฿", "Fixed");

        assertEquals(2, plan.expensesToAdd.size());
        assertEquals(0, plan.expensesSkipped);
        assertEquals(Collections.singletonMap("Pets", "Fixed"), plan.categoriesToAdd);
        assertEquals("2026-01-03", plan.firstDate);
        assertEquals("2026-01-04", plan.lastDate);
    }

    @Test
    public void duplicatesAreSkippedByCount() {
        // File has the same coffee three times on one day; app already has two of them
        BackupFormat.Parsed p = BackupFormat.parse("Category,Item,Amount,Date\n"
                + "Other,Coffee,60.0,05 Jan 2026\n"
                + "Other,Coffee,60.0,05 Jan 2026\n"
                + "Other,Coffee,60.0,05 Jan 2026\n"
                + "Other,Tea,50.0,05 Jan 2026\n");
        List<Expense> app = Arrays.asList(
                exp("2026-01-05", "Other", "Coffee", 60),
                exp("2026-01-05", "other", "coffee ", 60.0));   // case/space differences still match
        ImportPlan plan = ImportPlan.build(p, app, null, null, DEFAULT_CATS, "฿", "Fixed");

        assertEquals(2, plan.expensesSkipped);
        assertEquals(2, plan.expensesToAdd.size());   // one coffee + tea
    }

    @Test
    public void importingSameFileTwiceAddsNothing() {
        String csv = "Category,Item,Amount,Date\nGroceries,Milk,45.0,03 Jan 2026\n";
        List<Expense> app = new ArrayList<>();
        for (BackupFormat.ExpenseRec r :
                ImportPlan.build(BackupFormat.parse(csv), app, null, null, DEFAULT_CATS, "฿", "Fixed").expensesToAdd) {
            app.add(exp(r.isoDate, r.category, r.item, r.amount));
        }
        ImportPlan second = ImportPlan.build(BackupFormat.parse(csv), app, null, null, DEFAULT_CATS, "฿", "Fixed");
        assertEquals(0, second.totalToAdd());
        assertEquals(1, second.expensesSkipped);
    }

    @Test
    public void accountsMatchByNameAndItemsDeduplicate() {
        BackupFormat.Parsed p = BackupFormat.parse("AccountType,AccountName,Date,Item,Category,Amount,Note\n"
                + "Project,Condo build,05 Feb 2026,Tiles,Materials,1200.0,\n"
                + "Project,Condo build,06 Feb 2026,Paint,Materials,400.0,\n"
                + "Travel,New trip,10 Mar 2026,Hotel,Lodging,900.0,\n");
        List<AccountEntity> accounts = Collections.singletonList(acct(7, "condo build"));
        List<AccountItemEntity> items = Collections.singletonList(
                aitem(7, "05 Feb 2026", "Tiles", "Materials", 1200));

        ImportPlan plan = ImportPlan.build(p, null, accounts, items, DEFAULT_CATS, "฿", "Fixed");
        assertEquals(1, plan.accountsToCreate.size());
        assertEquals("New trip", plan.accountsToCreate.get(0).name);
        assertEquals(1, plan.accountItemsSkipped);
        assertEquals(2, plan.accountItemsToAdd.size());
    }

    @Test
    public void backupCategoriesKeepTheirTagsAndCurrencyOnlyForEmptyApp() {
        String text = BackupFormat.writeBackup("$",
                Collections.singletonMap("Pets", "Discretionary"),
                Collections.<BackupFormat.AccountRec>emptyList(),
                Collections.singletonList(new BackupFormat.ExpenseRec("2026-01-03", "Pets", "Food", 10)),
                Collections.<BackupFormat.AccountItemRec>emptyList(), new Date());

        ImportPlan empty = ImportPlan.build(BackupFormat.parse(text), null, null, null,
                DEFAULT_CATS, "฿", "Fixed");
        assertEquals("Discretionary", empty.categoriesToAdd.get("Pets"));
        assertEquals("$", empty.currencyToApply);

        ImportPlan notEmpty = ImportPlan.build(BackupFormat.parse(text),
                Collections.singletonList(exp("2026-02-01", "Rent", "x", 1)), null, null,
                DEFAULT_CATS, "฿", "Fixed");
        assertNull(notEmpty.currencyToApply);
        assertEquals("$", notEmpty.currencyInFile);
    }

    @Test
    public void newQuotedAccountsExportReimports() {
        String csv = BackupFormat.ACC_HEADER + "\n"
                + BackupFormat.row("Project", "Condo, phase 2 (archived)", "05 Feb 2026",
                        "Tiles, grout", "Materials", BackupFormat.plainAmount(1200), "paid, cash");
        BackupFormat.Parsed p = BackupFormat.parse(csv);
        assertTrue(p.badLines.isEmpty());
        assertEquals("Condo, phase 2", p.accounts.get(0).name);
        assertTrue(p.accounts.get(0).archived);
        assertEquals("Tiles, grout", p.accountItems.get(0).item);
        assertEquals("paid, cash", p.accountItems.get(0).note);
    }

    @Test
    public void previewDescribesThePlan() {
        BackupFormat.Parsed p = BackupFormat.parse("Category,Item,Amount,Date\n"
                + "Groceries,Milk,45.0,03 Jan 2026\n"
                + "Pets,Cat food,300.0,13 Oct 2026\n"
                + "broken\n");
        ImportPlan plan = ImportPlan.build(p,
                Collections.singletonList(exp("2026-01-03", "Groceries", "Milk", 45)),
                null, null, DEFAULT_CATS, "฿", "Fixed");
        String text = plan.describe("฿");
        assertEquals("Import Daily Living export", plan.title());
        assertTrue(text, text.contains("Dates: 03 Jan 2026 – 13 Oct 2026"));
        assertTrue(text, text.contains("New entries to add: 1\n"));
        assertTrue(text, text.contains("Already in the app (skipped): 1"));
        assertTrue(text, text.contains("New categories: Pets"));
        assertTrue(text, text.contains("Lines that couldn't be read: 1 (line 4)"));
        assertTrue(text, text.endsWith("Nothing already in the app is changed or deleted."));
    }

    @Test
    public void fullBackupRestoresTagsOfExistingCategories() {
        Map<String, String> backupCats = new LinkedHashMap<>();
        backupCats.put("Groceries", "Fixed");          // built-in, same tag: no change
        backupCats.put("snacks", "Discretionary");     // exists as Fixed (case differs)
        backupCats.put("Pharmacy", "Necessity");       // exists, already Necessity
        backupCats.put("Pets", "Discretionary");       // new
        String text = BackupFormat.writeBackup("฿", backupCats,
                Collections.<BackupFormat.AccountRec>emptyList(),
                Collections.<BackupFormat.ExpenseRec>emptyList(),
                Collections.<BackupFormat.AccountItemRec>emptyList(), new Date());

        Map<String, String> app = new LinkedHashMap<>(DEFAULT_CATS);
        app.put("Snacks", "Fixed");
        app.put("Pharmacy", "Necessity");

        ImportPlan plan = ImportPlan.build(BackupFormat.parse(text), null, null, null, app, "฿", "Fixed");
        assertEquals(Collections.singletonMap("Snacks", "Discretionary"), plan.categoriesToRetag);
        assertEquals(Collections.singletonMap("Pets", "Discretionary"), plan.categoriesToAdd);
        String preview = plan.describe("฿");
        assertTrue(preview, preview.contains("Tags restored from backup: Snacks → Discretionary"));
        assertTrue(preview, preview.endsWith("only the tags listed above are updated."));
    }

    @Test
    public void csvImportNeverChangesExistingTags() {
        Map<String, String> app = new LinkedHashMap<>(DEFAULT_CATS);
        app.put("Snacks", "Discretionary");
        ImportPlan plan = ImportPlan.build(BackupFormat.parse("Category,Item,Amount,Date\n"
                        + "Snacks,Chips,30.0,03 Jan 2026\n"),
                null, null, null, app, "฿", "Fixed");
        assertTrue(plan.categoriesToRetag.isEmpty());
        assertTrue(plan.categoriesToAdd.isEmpty());
    }

    @Test
    public void plainAmounts() {
        assertEquals("1500.5", BackupFormat.plainAmount(1500.5));
        assertEquals("10000000", BackupFormat.plainAmount(1.0E7));
        assertEquals("45", BackupFormat.plainAmount(45.0));
        assertEquals("0.1", BackupFormat.plainAmount(0.1));
    }
}
