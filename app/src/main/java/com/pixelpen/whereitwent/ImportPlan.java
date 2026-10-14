package com.pixelpen.whereitwent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Works out what an import would add (v3.2). Never removes or changes any
 * entry already in the app. The one thing a full backup may change is a
 * category's tag, so that restoring a backup restores the tags too.
 *
 * Duplicates are matched by count: if the app already has two identical
 * entries (same date, category, item, amount) and the file has three, one is
 * added. Importing the same file twice therefore adds nothing the second time,
 * while genuine repeats (two coffees on the same day) are kept.
 *
 * Pure Java, no Android imports, so it can be unit-tested off-device.
 */
public final class ImportPlan {

    public final BackupFormat.Kind kind;

    public final List<BackupFormat.ExpenseRec> expensesToAdd = new ArrayList<>();
    public final List<BackupFormat.AccountRec> accountsToCreate = new ArrayList<>();
    public final List<BackupFormat.AccountItemRec> accountItemsToAdd = new ArrayList<>();
    /** Categories not yet in the app, name -> tag. */
    public final Map<String, String> categoriesToAdd = new LinkedHashMap<>();
    /** Existing categories whose tag a full backup changes, name (as in the app) -> new tag. */
    public final Map<String, String> categoriesToRetag = new LinkedHashMap<>();

    /**
     * v3.3: trip details from the file for accounts already in the app. The
     * trip currency is only filled in for an account with no currency and no
     * entries yet (so no stored amount changes meaning); a journey name only
     * where the account has none.
     */
    public static final class TripUpdate {
        public final long accountId;
        public final String name;
        public final BackupFormat.AccountRec from;
        public final boolean currency, journey;
        TripUpdate(long id, String name, BackupFormat.AccountRec from, boolean currency, boolean journey) {
            this.accountId = id; this.name = name; this.from = from;
            this.currency = currency; this.journey = journey;
        }
    }
    public final List<TripUpdate> tripUpdates = new ArrayList<>();

    public int expensesSkipped;
    public int accountItemsSkipped;
    public final List<Integer> badLines;

    /** Currency from a full backup, applied only when the app has no entries yet. */
    public String currencyToApply;
    public String currencyInFile;
    /** Settings currency code to apply with it (full backup v2), or null. */
    public String currencyCodeToApply;

    /** Earliest and latest ISO date of everything in the file, or null. */
    public String firstDate, lastDate;

    private ImportPlan(BackupFormat.Parsed parsed) {
        this.kind = parsed.kind;
        this.badLines = parsed.badLines;
    }

    public int totalToAdd() {
        return expensesToAdd.size() + accountItemsToAdd.size();
    }

    public int totalInFile() {
        return totalToAdd() + expensesSkipped + accountItemsSkipped;
    }

    /**
     * @param existingCategories    categories already in the app, name -> current tag
     * @param defaultTag            tag for categories the file doesn't give one for
     */
    public static ImportPlan build(BackupFormat.Parsed parsed,
                                   Collection<Expense> existingExpenses,
                                   Collection<AccountEntity> existingAccounts,
                                   Collection<AccountItemEntity> existingItems,
                                   Map<String, String> existingCategories,
                                   String currentCurrency,
                                   String defaultTag) {
        return build(parsed, existingExpenses, existingAccounts, existingItems,
                existingCategories, currentCurrency, null, defaultTag);
    }

    /** @param currentCode the Settings currency code (v3.3), or null */
    public static ImportPlan build(BackupFormat.Parsed parsed,
                                   Collection<Expense> existingExpenses,
                                   Collection<AccountEntity> existingAccounts,
                                   Collection<AccountItemEntity> existingItems,
                                   Map<String, String> existingCategories,
                                   String currentCurrency,
                                   String currentCode,
                                   String defaultTag) {

        ImportPlan plan = new ImportPlan(parsed);

        // ---------- Daily Living ----------
        Map<String, Integer> have = new HashMap<>();
        if (existingExpenses != null) {
            for (Expense e : existingExpenses) {
                String iso = DateUtils.toIso(e.date);
                if (iso == null) continue;
                bump(have, expenseKey(iso, e.category, e.description, e.amount));
            }
        }
        for (BackupFormat.ExpenseRec r : parsed.expenses) {
            plan.noteDate(r.isoDate);
            String key = expenseKey(r.isoDate, r.category, r.item, r.amount);
            if (take(have, key)) plan.expensesSkipped++;
            else plan.expensesToAdd.add(r);
        }

        // ---------- Categories ----------
        Set<String> knownCats = new HashSet<>();
        Map<String, String> appName = new HashMap<>();   // normalised -> name as in the app
        Map<String, String> appTag = new HashMap<>();    // normalised -> current tag
        if (existingCategories != null) {
            for (Map.Entry<String, String> c : existingCategories.entrySet()) {
                knownCats.add(norm(c.getKey()));
                appName.put(norm(c.getKey()), c.getKey());
                appTag.put(norm(c.getKey()), c.getValue());
            }
        }
        // A full backup is the authority on tags: new categories get its tag,
        // and existing ones whose tag differs are updated to match it.
        for (Map.Entry<String, String> c : parsed.categories.entrySet()) {
            String k = norm(c.getKey());
            if (knownCats.add(k)) {
                plan.categoriesToAdd.put(c.getKey(), c.getValue());
            } else if (appTag.containsKey(k) && !norm(appTag.get(k)).equals(norm(c.getValue()))) {
                plan.categoriesToRetag.put(appName.get(k), c.getValue());
            }
        }
        for (BackupFormat.ExpenseRec r : plan.expensesToAdd) {
            if (knownCats.add(norm(r.category))) plan.categoriesToAdd.put(r.category, defaultTag);
        }

        // ---------- Accounts ----------
        Map<Long, String> nameById = new HashMap<>();
        Set<String> knownAccounts = new HashSet<>();
        Map<String, AccountEntity> appAccount = new HashMap<>();
        if (existingAccounts != null) {
            for (AccountEntity a : existingAccounts) {
                nameById.put(a.id, a.name);
                knownAccounts.add(norm(a.name));
                appAccount.put(norm(a.name), a);
            }
        }
        Set<Long> withEntries = new HashSet<>();
        if (existingItems != null) {
            for (AccountItemEntity it : existingItems) withEntries.add(it.accountId);
        }
        for (BackupFormat.AccountRec a : parsed.accounts) {
            if (knownAccounts.add(norm(a.name))) {
                plan.accountsToCreate.add(a);
                continue;
            }
            AccountEntity inApp = appAccount.get(norm(a.name));
            if (inApp == null || !a.hasTripDetails()) continue;
            boolean cur = (notBlank(a.foreignCurrency) || notBlank(a.homeCurrency))
                    && !notBlank(inApp.foreignCurrency) && !notBlank(inApp.homeCurrency)
                    && !withEntries.contains(inApp.id);
            boolean jour = notBlank(a.journey) && !notBlank(inApp.journey);
            if (cur || jour) plan.tripUpdates.add(new TripUpdate(inApp.id, inApp.name, a, cur, jour));
        }

        // ---------- Account items ----------
        Map<String, Integer> haveItems = new HashMap<>();
        if (existingItems != null) {
            for (AccountItemEntity it : existingItems) {
                String acct = nameById.get(it.accountId);
                String iso = DateUtils.toIso(it.date);
                if (acct == null || iso == null) continue;
                bump(haveItems, itemKey(acct, iso, it.item, it.category, it.amount));
            }
        }
        for (BackupFormat.AccountItemRec r : parsed.accountItems) {
            plan.noteDate(r.isoDate);
            String key = itemKey(r.accountName, r.isoDate, r.item, r.category, r.amount);
            if (take(haveItems, key)) plan.accountItemsSkipped++;
            else plan.accountItemsToAdd.add(r);
            // An item for an account the file never declared (shouldn't happen) still gets one
            if (knownAccounts.add(norm(r.accountName))) {
                plan.accountsToCreate.add(new BackupFormat.AccountRec(r.accountName, "Custom", false));
            }
        }

        // ---------- Currency ----------
        plan.currencyInFile = parsed.currencySymbol;
        boolean appEmpty = (existingExpenses == null || existingExpenses.isEmpty())
                && (existingItems == null || existingItems.isEmpty());
        if (parsed.currencyCode != null && currentCode != null) {
            // v2 backups name the currency exactly (US vs Australian dollar)
            if (appEmpty && !parsed.currencyCode.equalsIgnoreCase(currentCode)) {
                plan.currencyCodeToApply = parsed.currencyCode;
                plan.currencyToApply = parsed.currencySymbol != null
                        ? parsed.currencySymbol : parsed.currencyCode;
            }
        } else if (parsed.currencySymbol != null && appEmpty
                && !parsed.currencySymbol.equals(currentCurrency)) {
            plan.currencyToApply = parsed.currencySymbol;
        }
        return plan;
    }

    // --------------------------------------------------------
    // PREVIEW TEXT
    // --------------------------------------------------------

    /** Title for the preview dialog. */
    public String title() {
        switch (kind) {
            case FULL_BACKUP:      return "Restore from full backup";
            case DAILY_LIVING_CSV: return "Import Daily Living export";
            case ACCOUNTS_CSV:     return "Import Accounts export";
            default:               return "Import";
        }
    }

    /** Plain-text description of what the import would do. */
    public String describe(String currentCurrency) {
        StringBuilder sb = new StringBuilder();
        if (firstDate != null) {
            sb.append("Dates: ").append(ui(firstDate)).append(" – ").append(ui(lastDate)).append("\n\n");
        }

        sb.append("New entries to add: ").append(fmt(totalToAdd())).append('\n');
        if (!expensesToAdd.isEmpty() && !accountItemsToAdd.isEmpty()) {
            sb.append("   Daily Living ").append(fmt(expensesToAdd.size()))
                    .append(" · Accounts ").append(fmt(accountItemsToAdd.size())).append('\n');
        }
        int skipped = expensesSkipped + accountItemsSkipped;
        if (skipped > 0) {
            sb.append("Already in the app (skipped): ").append(fmt(skipped)).append('\n');
        }

        if (!accountsToCreate.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (BackupFormat.AccountRec a : accountsToCreate) {
                names.add(a.archived ? a.name + " (archived)" : a.name);
            }
            sb.append("\nNew accounts: ").append(list(names)).append('\n');
        }
        List<String> trips = new ArrayList<>();
        for (BackupFormat.AccountRec a : accountsToCreate) {
            if (a.hasTripDetails()) trips.add(a.name);
        }
        for (TripUpdate u : tripUpdates) trips.add(u.name);
        if (!trips.isEmpty()) {
            sb.append("\nTrip currencies and journeys: ").append(list(trips)).append('\n');
        }
        if (!categoriesToAdd.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, String> c : categoriesToAdd.entrySet()) {
                names.add(kind == BackupFormat.Kind.FULL_BACKUP
                        ? c.getKey() + " (" + c.getValue() + ")" : c.getKey());
            }
            sb.append("\nNew categories: ").append(list(names)).append('\n');
            if (kind != BackupFormat.Kind.FULL_BACKUP) {
                sb.append("   Tagged ").append(categoriesToAdd.values().iterator().next())
                        .append(" for now; change in Edit Categories.\n");
            }
        }

        if (!categoriesToRetag.isEmpty()) {
            List<String> names = new ArrayList<>();
            for (Map.Entry<String, String> c : categoriesToRetag.entrySet()) {
                names.add(c.getKey() + " → " + c.getValue());
            }
            sb.append("\nTags restored from backup: ").append(list(names)).append('\n');
        }

        if (currencyToApply != null) {
            sb.append("\nCurrency will be set to ").append(currencyToApply).append('\n');
        } else if (currencyInFile != null && !currencyInFile.equals(currentCurrency)) {
            sb.append("\nCurrency in file is ").append(currencyInFile)
                    .append("; yours stays ").append(currentCurrency).append('\n');
        }

        if (!badLines.isEmpty()) {
            sb.append("\nLines that couldn't be read: ").append(badLines.size()).append(" (line ");
            for (int i = 0; i < Math.min(5, badLines.size()); i++) {
                if (i > 0) sb.append(", ");
                sb.append(badLines.get(i));
            }
            if (badLines.size() > 5) sb.append(", …");
            sb.append(")\n");
        }

        sb.append(categoriesToRetag.isEmpty() && tripUpdates.isEmpty()
                ? "\nNothing already in the app is changed or deleted."
                : "\nNo entries are changed or deleted; only the "
                        + (tripUpdates.isEmpty() ? "tags" : "details") + " listed above are updated.");
        return sb.toString();
    }

    private static String ui(String iso) {
        String s = DateUtils.isoToUi(iso);
        return s == null ? iso : s;
    }

    private static String fmt(int n) {
        return String.format(Locale.US, "%,d", n);
    }

    private static String list(List<String> names) {
        // (String.join needs Android 8; the app supports Android 7)
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(6, names.size());
        for (int i = 0; i < shown; i++) {
            if (i > 0) sb.append(", ");
            sb.append(names.get(i));
        }
        if (names.size() > shown) sb.append(" and ").append(names.size() - shown).append(" more");
        return sb.toString();
    }

    // --------------------------------------------------------

    private void noteDate(String iso) {
        if (firstDate == null || iso.compareTo(firstDate) < 0) firstDate = iso;
        if (lastDate == null || iso.compareTo(lastDate) > 0) lastDate = iso;
    }

    static String expenseKey(String iso, String category, String item, double amount) {
        return iso + "|" + norm(category) + "|" + norm(item) + "|" + amountKey(amount);
    }

    static String itemKey(String account, String iso, String item, String category, double amount) {
        return norm(account) + "|" + iso + "|" + norm(item) + "|" + norm(category) + "|" + amountKey(amount);
    }

    private static String amountKey(double v) {
        return String.format(Locale.US, "%.2f", v);
    }

    private static boolean notBlank(String s) {
        return s != null && !s.trim().isEmpty();
    }

    private static String norm(String s) {
        if (s == null) return "";
        String t = s.trim();
        if ("null".equals(t)) return "";
        return t.toLowerCase(Locale.ROOT);
    }

    private static void bump(Map<String, Integer> m, String k) {
        Integer v = m.get(k);
        m.put(k, v == null ? 1 : v + 1);
    }

    /** Uses up one existing match for {@code k}; true if there was one. */
    private static boolean take(Map<String, Integer> m, String k) {
        Integer v = m.get(k);
        if (v == null || v == 0) return false;
        m.put(k, v - 1);
        return true;
    }
}
